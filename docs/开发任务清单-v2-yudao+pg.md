# 开发任务清单（v2 | yudao 单体 + Postgres）

适用范围：基于 [技术方案-选品Agent任务平台-v2-yudao+pg.md](file:///d:/work/workspace/crob-ai-agent/docs/%E6%8A%80%E6%9C%AF%E6%96%B9%E6%A1%88-%E9%80%89%E5%93%81Agent%E4%BB%BB%E5%8A%A1%E5%B9%B3%E5%8F%B0-v2-yudao%2Bpg.md) 的单机 MVP 实现  
目标：新 session 可直接按清单开工，优先跑通两个场景端到端闭环（MARKET_ANALYSIS + PROFIT_MODEL）

---

## 0. 仓库结构（先创建）

- [ ] 创建目录：java-control/ web-admin/ python-executor/ deploy/ datasets/

验收：
- [ ] 新 session 打开仓库能一眼看到三个工程入口（Java/Vue/Python）与部署目录

---

## 1. 基础设施（deploy/ + docker-compose）

- [ ] 编写 deploy/docker-compose.yml（java-control / python-executor / postgres / redis / minio）
- [ ] Postgres 初始化（创建库、用户、基础参数；pgvector 可选）
- [ ] Redis 初始化（内存与淘汰策略，nonce/lock 专用 key 前缀约定）
- [ ] MinIO 初始化（bucket：evidence / audit-pack；对外不暴露）
- [ ] 宿主机 Nginx 反代（只反代 Java；SSE 关闭缓冲与加长超时；禁止 /internal 透出）
- [ ] 环境变量规范：
  - [ ] SILICONFLOW_API_KEY
  - [ ] INTERNAL_HMAC_SECRET
  - [ ] Postgres/Redis/MinIO 连接信息

验收：
- [ ] docker-compose 一键启动成功，重启后数据持久化正常
- [ ] 公网仅可访问 Nginx 80/443（转发到 Java）

---

## 2. Java 控制面（yudao 单体落地）

### 2.1 脚手架落地
- [ ] 在 java-control/ 引入 yudao 单体脚手架（或等价的 yudao 单体工程形态）
- [ ] 适配 Postgres（数据源、方言、初始化脚本、类型映射）
- [ ] 前后端分离 API 形态：统一 /api 前缀；页面由 web-admin 提供

验收：
- [ ] Java 服务启动成功并连通 Postgres

### 2.2 数据库迁移（Java 负责）
- [ ] 选择迁移工具（建议 Flyway）
- [ ] 建表迁移（禁止外键；所有表包含 created_at/updated_at/created_by/updated_by）
- [ ] 表：task / attempt / task_event / usage_event / task_result / artifact
- [ ] 表：provider_config / model_catalog / pricing / policy_template / scenario_default_policy
- [ ] 索引与唯一约束按技术方案落地

验收：
- [ ] 初始化与升级迁移可重复执行

### 2.3 通用基础能力
- [ ] 雪花ID生成器（task_id/attempt_id/artifact_id 统一由 Java 生成）
- [ ] 统一错误码（E_*）与对外错误响应结构
- [ ] 统一审计字段写入（created_by/updated_by 主体表达）

### 2.4 配置管理（先做最小可用）
- [ ] provider_config CRUD（非敏感：enabled/base_url/timeout_ms/key_env）
- [ ] model_catalog CRUD（强校验；启用/禁用）
- [ ] pricing 管理（版本化；仅影响未来新 attempt；usage_event 回填 cost_fen）
- [ ] policy_template 管理（启用模板不可编辑；复制生成新版本）
- [ ] scenario_default_policy 管理（按场景默认 policy）

验收：
- [ ] 能配置 siliconflow provider + 3-5 个模型 + pricing + 两个场景默认 policy

### 2.5 对外任务 API（v1）
- [ ] POST /api/tasks（Idempotency-Key）
  - [ ] 缺参：task=WAITING_INPUT + ASK_CLARIFY
  - [ ] 完整：门禁→创建 attempt=QUEUED→下发 Python
  - [ ] 门禁失败：task=REJECTED
- [ ] POST /api/tasks/{task_id}/inputs（补参）
  - [ ] 仍缺参：继续 WAITING_INPUT
  - [ ] 补齐：门禁→创建 attempt
  - [ ] 非法：REJECTED
- [ ] GET /api/tasks/{task_id}（权威聚合状态 + current/latest_success + report_url）
- [ ] GET /api/tasks/{task_id}/attempts
- [ ] POST /api/tasks/{task_id}/cancel
- [ ] POST /api/tasks/{task_id}/rerun（Idempotency-Key + override_constraints + 可选 policy_version）
- [ ] GET /api/tasks/{task_id}/stream（SSE：L6-only + PING）
- [ ] GET /tasks/{task_id}/report（Markdown → HTML）
- [ ] GET /api/tasks/{task_id}/artifacts/{artifact_id}（Java 代理 MinIO）

验收：
- [ ] WAITING_INPUT/REJECTED/FAILED/SUCCEEDED 的行为与提示一致
- [ ] rerun 不覆盖历史成功 attempt（latest_success 保留）

### 2.6 内部接口与安全
- [ ] HMAC 校验中间件（60s 窗口 + Redis nonce 去重）
- [ ] POST /internal/task-events（接收 Python attempt_event/usage_event）
- [ ] 事件幂等与状态迁移校验（终态不回退）
- [ ] 超时扫描（FAILED：E_TIMEOUT/E_EXECUTOR_LOST）

验收：
- [ ] 重复上报不会重复扣费/重复变更状态

---

## 3. web-admin（Vue3 管理端，参考 yudao）

- [ ] 初始化 Vue3 管理端工程（参考 yudao Vue3 管理端结构、权限路由、布局）
- [ ] 页面：任务列表/任务详情/attempt 时间线/成本统计（最小可用）
- [ ] 页面：provider/model/policy/pricing 配置页（最小可用）
- [ ] SSE 展示：L6 输出实时展示（断线后轮询状态）

验收：
- [ ] 管理端可完成：创建任务→补参→查看执行→查看报告→rerun

---

## 4. Python 执行面（python-executor）

### 4.1 基础能力
- [ ] /internal/execute 接口（接收 task_id/attempt_id/scenario/inputs/effective_constraints_json）
- [ ] Redis attempt 锁（TTL + 续约）
- [ ] cancel 检查点（step 边界 + L6 流循环）
- [ ] attempt 内 seq 生成器（事件/usage 共用序列）

### 4.2 Provider 插件框架（entry points）
- [ ] entry points：crob_agent.providers 自动发现
- [ ] ProviderAdapter 接口（sync + stream）
- [ ] 统一 LLMChunk（delta/is_final/finish_reason/usage/provider/model）
- [ ] 错误映射（统一错误码）

### 4.3 SiliconFlow 插件（第一期）
- [ ] base_url 默认 https://api.siliconflow.cn/v1（可覆盖）
- [ ] api_key 仅从环境变量读取（SILICONFLOW_API_KEY）
- [ ] chat_completions 与 stream_chat_completions（stream=True）
- [ ] usage 解析（同步/流式末尾）

### 4.4 模型网关（执行层内部）
- [ ] Router：constraints.llm_route.by_layer + fallback
- [ ] LLMClient：超时、重试（1 次白名单）、usage 采集

### 4.5 事件上报（Python → Java）
- [ ] HMAC 签名实现
- [ ] attempt_event 上报：ATTEMPT_STARTED/STEP_CHANGED/L6_START/L6_END/SUCCEEDED/FAILED/CANCELLED
- [ ] usage_event：每次 LLM 调用结束上报一次（USAGE_REPORTED）

### 4.6 落库（Postgres）
- [ ] task_result：report_md（分析型强结构）+ result_json（schema v1）
- [ ] MARKET_ANALYSIS：静态数据集读取（精确 slug 匹配），找不到 FAILED(E_DATA_UNAVAILABLE)
- [ ] PROFIT_MODEL：规则计算为主（可复算）+ L6 负责解释与排版（stream）

验收：
- [ ] 两个场景均可 SUCCEEDED 且报告含证据表/溯源信息

---

## 5. 静态数据集（datasets/）

- [ ] 目录：datasets/market_analysis/{platform}/{market}/
- [ ] 文件：{keyword_slug}__v{yyyyMMdd}.json
- [ ] v1：仅精确 slug 匹配

验收：
- [ ] 放入一份数据集文件即可驱动 MARKET_ANALYSIS 跑通

---

## 6. 端到端验收用例（必须全通过）

- [ ] MARKET_ANALYSIS：完整输入 → QUEUED → RUNNING（stream）→ SUCCEEDED → report 可渲染
- [ ] MARKET_ANALYSIS：缺 platform → WAITING_INPUT → 补参 → SUCCEEDED
- [ ] MARKET_ANALYSIS：数据集缺失 → FAILED(E_DATA_UNAVAILABLE)
- [ ] PROFIT_MODEL：price<cost → 二次确认 YES_CONTINUE → SUCCEEDED（报告提示负利润风险）
- [ ] cancel：RUNNING 时取消 → CANCELLED（不生成报告）
- [ ] rerun：SUCCEEDED 后修改 L6 模型 rerun → 新 attempt，不覆盖历史成功
- [ ] HMAC：内部接口签名错误被拒绝
- [ ] 幂等：同 Idempotency-Key 创建任务只生成一个 task

