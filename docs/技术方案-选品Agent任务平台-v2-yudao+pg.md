# 技术方案：选品 Agent 任务平台（v2 | yudao 单体 + Postgres）

面向对象：内部产品/研发（新 session 可直接开工）  
部署前提：单台阿里云 ECS（4C16G），宿主机已有 Nginx，优先开源自建组件（Postgres/Redis/MinIO）  
UI 形态：前后端分离，前端 Vue3（管理端起步可参考 yudao Vue3 管理端）

---

## 0. 开工入口（新 session 先读这里）

1. 阅读本文档（本文件）
2. 阅读开发任务清单：
   - docs/开发任务清单-v2-yudao+pg.md
3. 目标：先跑通两个场景的端到端闭环（MARKET_ANALYSIS + PROFIT_MODEL）
4. 约束：单机资源有限，Java 控制面先单体，后续再考虑拆分；数据库选 Postgres，不走 MySQL

---

## 1. 关键决策（最终定型）

### 1.1 总体架构

- 控制面（Java）：对外唯一入口（Web/API）、权威状态、门禁、配置、审计、成本换算
- 执行面（Python）：状态机编排、模型网关（provider 插件）、结果落库、事件上报
- Nginx：公网入口，仅反代 Java，对 SSE 做必要配置

### 1.2 数据库与存储

- 数据库：Postgres（固定选型；可选 pgvector）
- 缓存/锁：Redis（attempt 执行占用锁、nonce 防重放、轻量限流）
- 对象存储：MinIO（evidence/audit-pack），对外不暴露，Java 代理下载
- 表设计：禁止外键；所有实体包含 created_at/updated_at/created_by/updated_by；ID 使用雪花ID（Java 统一生成）

### 1.3 体验与任务控制

- 流式：仅 L6 输出生成 stream；Java SSE 转发 + PING；不落 delta 日志；断线后轮询状态，成功后看 report
- 取消：取消即终止，不生成部分报告，不支持 resume
- retry/rerun：
  - retry：最多 1 次，白名单瞬时错误（429/503/504/超时）
  - rerun：同 task_id 新 attempt_id，允许 override_constraints
- 超时：Java 判定超时，直接 FAILED（E_TIMEOUT/E_EXECUTOR_LOST）
- 幂等：创建任务与 rerun 支持 Idempotency-Key；事件/usage 以 (task_id,attempt_id,seq) 幂等；执行占用用 Redis 锁

### 1.4 yudao 脚手架使用边界

目标是“快速获得成熟的管理后台能力”，但不把单机 MVP 拖入微服务运维：

- Java 控制面：采用 yudao 的单体形态作为脚手架起点（RBAC/菜单/租户/配置/操作日志等能力）
- 不使用 yudao-cloud 的多服务拆分作为 v1 落地形态（可作为未来演进参考）
- 前端：采用/参考 yudao Vue3 管理端工程（路由/权限/布局/组件库），实现任务平台管理页

---

## 2. 代码库布局建议（新 session 直接照这个建）

推荐在仓库根目录建立：

- java-control/  
  - Java 单体服务（基于 yudao 单体脚手架改造）
- web-admin/  
  - Vue3 管理端（参考/复用 yudao Vue3 管理端结构）
- python-executor/  
  - Python 执行层（FastAPI + provider 插件）
- deploy/  
  - docker-compose、初始化脚本、Nginx 配置片段
- datasets/  
  - 静态数据集（MARKET_ANALYSIS v1）

---

## 3. 控制面（Java）职责与接口边界

### 3.1 对外接口（摘要）

- POST /api/tasks（Idempotency-Key；缺参创建 WAITING_INPUT）
- POST /api/tasks/{task_id}/inputs（补参；可能进入 QUEUED）
- GET /api/tasks/{task_id}（权威聚合状态）
- GET /api/tasks/{task_id}/attempts
- POST /api/tasks/{task_id}/cancel
- POST /api/tasks/{task_id}/rerun（Idempotency-Key + override_constraints）
- GET /api/tasks/{task_id}/stream?attempt_id=xxx（SSE：L6-only + PING）
- GET /tasks/{task_id}/report?attempt_id=xxx（渲染 report_md）
- GET /api/tasks/{task_id}/artifacts/{artifact_id}（代理 MinIO 下载）

### 3.2 内部接口（HMAC）

- Java → Python：POST /internal/execute
- Python → Java：POST /internal/task-events（含 attempt_event 与 usage_event）

---

## 4. 执行面（Python）职责与接口边界

### 4.1 内部执行入口

- POST /internal/execute（接收 task_id/attempt_id/scenario/inputs/effective_constraints_json）

### 4.2 provider 插件（entry points）

- group：crob_agent.providers
- v1 仅要求 chat_completions + stream_chat_completions
- SiliconFlow 为首个 provider：
  - base_url 默认 https://api.siliconflow.cn/v1
  - API Key 仅环境变量 SILICONFLOW_API_KEY

---

## 5. 数据模型（Postgres，无外键）

核心表（均包含四审计字段）：

- task（含 WAITING_INPUT/REJECTED）
- attempt（含 price_version_snapshot、effective_constraints_json）
- task_event（审计事件，不含 L6_DELTA）
- usage_event（每次 LLM 调用计量，Java 回填 cost_fen）
- task_result（report_md + result_json）
- artifact（artifact_id → bucket/key，用于 Java 代理）

配置表：

- provider_config（非敏感）
- model_catalog（强校验）
- pricing（版本化，仅影响未来新 attempt）
- policy_template（不可变模板）
- scenario_default_policy（按场景默认 policy）

---

## 6. inputs 与补参（ASK_CLARIFY）

### 6.1 总规则

- 缺参：创建 task=WAITING_INPUT，不创建 attempt，不冻结预算
- 补参：POST /api/tasks/{task_id}/inputs；补齐后再门禁→创建 attempt
- 门禁失败/配置错误/非法值：task=REJECTED

### 6.2 MARKET_ANALYSIS v1

必填：

- category_or_keyword（需精确 slug 匹配静态数据集）
- platform（AMAZON/TEMU/TIKTOK_SHOP）
- market（US/UK/DE/FR/JP）

v1 规则：OTHER 直接 REJECTED

### 6.3 PROFIT_MODEL v1

必填：

- platform
- market
- shipping_mode（FBA/FBM；OTHER→REJECTED）
- cost_fen（分，int）
- price_fen（分，int）

price_fen < cost_fen：ASK_CLARIFY 二次确认（YES_CONTINUE / NO_EDIT）

---

## 7. 状态机与事件（v1）

### 7.1 task 状态（聚合态）

- WAITING_INPUT / REJECTED / QUEUED / RUNNING / SUCCEEDED / FAILED / CANCELLING / CANCELLED

task 指针：

- current_attempt_id
- latest_success_attempt_id

### 7.2 attempt 事件（落库）

- ATTEMPT_STARTED / STEP_CHANGED / L6_START / L6_END
- ATTEMPT_SUCCEEDED / ATTEMPT_FAILED / ATTEMPT_CANCELLED
- HEARTBEAT（可选：可只更新 last_heartbeat_at）
- USAGE_REPORTED（每次 LLM 调用结束上报一次）

幂等：unique(task_id,attempt_id,seq)，seq 为 attempt 内递增

---

## 8. 流式（SSE）规范（L6-only）

- 对外：Java SSE（/api/tasks/{task_id}/stream）
- 事件：L6_START / L6_DELTA / L6_END / ERROR / PING
- Java 发送 PING（10~15s），不落 delta 日志
- 断线：不续流；轮询 task 状态，SUCCEEDED 后查看 report

---

## 9. MVP 执行链路（两场景）

### 9.1 MARKET_ANALYSIS（静态数据集）

- datasets/market_analysis/{platform}/{market}/{keyword_slug}__v{yyyyMMdd}.json
- v1：精确 slug 匹配；找不到 → FAILED(E_DATA_UNAVAILABLE)

steps：

- MA_01_VALIDATE_INPUT
- MA_02_BUILD_QUERY_PLAN
- MA_03_FETCH_DATA（静态数据集）
- MA_04_TRUTH_VERIFY（L4/L5：溯源包装）
- MA_05_GENERATE_REPORT（L6 stream + L7 规则校验）

### 9.2 PROFIT_MODEL（规则计算为主）

steps：

- PM_01_VALIDATE_INPUT
- PM_02_LOAD_RATE_PROFILE（版本化）
- PM_03_CALCULATE_PNL（可复算，不调用 LLM）
- PM_04_SENSITIVITY_ANALYSIS
- PM_05_TRUTH_AND_PROVENANCE（L4/L5）
- PM_06_GENERATE_REPORT（L6 stream + L7 规则校验）

---

## 10. 主要错误码（v1）

- E_INPUT_INVALID / E_INPUT_MISSING
- E_POLICY_INVALID
- E_PROVIDER_CONFIG_INVALID
- E_PRICING_NOT_FOUND
- E_BUDGET_EXCEEDED / E_RATE_LIMITED
- E_DATA_UNAVAILABLE
- E_TIMEOUT / E_EXECUTOR_LOST
- E_PROVIDER_RATE_LIMIT / E_PROVIDER_UNAVAILABLE / E_PROVIDER_AUTH

