# Findings & Decisions

## MVP 范围（来自产品方案）

**做什么：** 市场分析 + 利润测算 + L1/L3/L4/L5 护栏 + L7 人工抽检
**不做什么：** 竞品监控、选品推荐、自动化质量评估、法律/投资建议

---

## 差距分析总览（2026-05-28 深度审查）

三个维度评估现有代码：✅ 完成 / ⚠️ 部分完成（需增强） / ❌ 缺失

---

## 一、Java 控制面 (java-control/)

### 1.1 Crob 业务模块 — 详细评估

| 功能 | 状态 | 评估 |
|------|------|------|
| POST /api/tasks（创建+Idempotency-Key） | ✅ | 完整：缺参→WAITING_INPUT，门禁失败→REJECTED，完整→QUEUED |
| POST /api/tasks/{id}/inputs（补参） | ✅ | 完整：merge inputs，再次校验，仅 WAITING_INPUT 可补参 |
| GET /api/tasks/{id} | ✅ | 完整：返回聚合状态+disposition+missingFields+reportUrl |
| GET /api/tasks/{id}/attempts | ✅ | 完整：按 attempt_id 降序 |
| POST /api/tasks/{id}/cancel | ⚠️ | 基本可用：设置 Redis cancel 标志+DB 状态。**缺 CANCELLING 中间态，直接 CANCELLED** |
| POST /api/tasks/{id}/rerun | ✅ | 完整：新 attempt + Idempotency-Key + override_constraints |
| GET /api/tasks/{id}/stream（SSE） | ⚠️ | 基本可用：Redis Stream 消费→Flux。**但使用 readOffset.latest() 会丢失历史 delta** |
| GET /tasks/{id}/report | ⚠️ | 基本可用：Markdown HTML 转义渲染。**但仅 escape，无 Markdown→HTML 解析** |
| GET /api/tasks/{id}/artifacts/{id} | ❌ | **缺失**：无 artifact 代理下载端点 |
| 超时扫描 | ❌ | **缺失**：无 E_TIMEOUT/E_EXECUTOR_LOST 判定 |
| RestTemplate Bean | ❌ | CrobExecutorClientImpl 注入 RestTemplate 但未找到 Bean 定义 |
| usage_event cost_fen 回填 | ❌ | **缺失**：usage_event 记录后未根据 pricing 表回填 cost |

### 1.2 内部事件处理

| 功能 | 状态 | 评估 |
|------|------|------|
| POST /internal/task-events | ✅ | 完整：attempt_event + usage_event 双通道 |
| 事件幂等（task_id+attempt_id+seq） | ✅ | 用 DuplicateKeyException 兜底 |
| 状态迁移校验 | ✅ | ATTEMPT_STARTED→RUNNING, SUCCEEDED/FAILED/CANCELLED |
| HEARTBEAT 处理 | ✅ | 更新 last_heartbeat_at |

### 1.3 HMAC 安全

| 功能 | 状态 | 评估 |
|------|------|------|
| InternalHmacFilter | ✅ | 完整：60s 窗口 + Redis nonce + body sha256 签名 |
| Java→Python HMAC 签名 | ✅ | CrobExecutorClientImpl 实现 |
| CachedBodyHttpServletRequest | ✅ | body 缓存支持重复读取 |

### 1.4 数据模型

| 表 | DO | Mapper | SQL | 状态 |
|----|-----|--------|-----|------|
| crob_task | ✅ | ✅ | ✅ | 完整对齐 |
| crob_attempt | ✅ | ✅ | ✅ | 完整对齐 |
| crob_task_event | ✅ | ✅ | ✅ | 完整对齐 |
| crob_usage_event | ✅ | ✅ | ✅ | 完整对齐 |
| crob_task_result | ✅ | ✅ | ✅ | 完整对齐 |
| crob_artifact | ❌ | ❌ | ✅ | SQL 有，Java DO/Mapper 缺失 |
| provider_config | ❌ | ❌ | ❌ | 技术方案要求但未实现 |
| model_catalog | ❌ | ❌ | ❌ | 技术方案要求但未实现 |
| pricing | ❌ | ❌ | ❌ | 技术方案要求但未实现 |
| policy_template | ❌ | ❌ | ❌ | 技术方案要求但未实现 |
| scenario_default_policy | ❌ | ❌ | ❌ | 技术方案要求但未实现 |

### 1.5 基础平台功能缺失（用户要求）

| 功能 | 状态 | 说明 |
|------|------|------|
| 租户管理 | ❌ | Crob-module-system 可能部分存在，需检查 |
| 部门管理 | ❌ | 同上 |
| 用户管理 | ❌ | 同上 |
| 登录认证 | ❌ | 同上 |
| 菜单权限/RBAC | ❌ | 同上 |
| 操作日志 | ❌ | 同上 |

### 1.6 芋道痕迹（需清除）

| 位置 | 严重程度 | 说明 |
|------|----------|------|
| CrobServerApplication.java | 高 | Javadoc 注释含 `doc.iocoder.cn`、"芋道源码" |
| README.md | 高 | 全文芋道/ruoyi branding |
| LICENSE | 高 | `Copyright (c) 2021 ruoyi-vue-pro` |
| .gitignore | 低 | `yudao-ui-app` 引用 |
| Framework pom target | 中 | target 目录含 `cn.iocoder.boot`/`yudao-*` 路径 |

---

## 二、Python 执行面 (python-executor/)

### 2.1 详细评估

| 功能 | 状态 | 评估 |
|------|------|------|
| /internal/execute + HMAC 校验 | ✅ | 完整 |
| MARKET_ANALYSIS 场景 | ⚠️ | 基本可用。**但缺少步骤分离（MA_01~MA_05），没有 L4 真实性验证步骤** |
| PROFIT_MODEL 场景 | ⚠️ | 基本可用。**缺少 PM_02_LOAD_RATE_PROFILE（费率版本化），直接硬编码计算** |
| CHAT 场景 | ✅ | 可用 |
| Redis Stream 流式输出 | ✅ | xadd delta 到 stream |
| HMAC 事件回调 Java | ✅ | java_client.py 实现 |
| cancel 检查 | ⚠️ | 仅在 `_check_cancel` 中检查，`stream_chat_completions` 内部不检查 |
| HEARTBEAT 事件 | ❌ | **缺失**：未定时发送 |
| USAGE_REPORTED 事件 | ❌ | **缺失**：LLM 调用后未上报 token 用量 |
| attempt 锁 | ✅ | Redis SET NX + TTL 1800s |
| task_result 落库 | ✅ | upsert 到 crob_task_result |
| L1 边界判断 | ❌ | **缺失**：直接根据 scenario 路由，没有 L1 意图分类/红线判定 |
| L4 真实性校验 | ❌ | **缺失**：无来源分级、交叉验证 |
| L5 溯源包装 | ❌ | **缺失**：输出数据无 data_point_id/source/confidence 标注 |
| 错误码映射 | ⚠️ | 部分：E_DATA_UNAVAILABLE, E_INPUT_INVALID, E_EXECUTOR_FAILED |

### 2.2 Python 代码质量

- ✅ HMAC 实现与 Java 完全一致（sha256 + canonical 格式）
- ✅ 无第三方框架依赖（仅 FastAPI + httpx + redis + psycopg）
- ✅ 包结构清晰，无芋道痕迹
- ❌ executor.py 中 `_post_attempt_event` 不等待 HTTP 响应（fire-and-forget），可能导致事件丢失

---

## 三、前端 (web-admin/crob-admin-web/)

### 3.1 Crob 业务页面

| 功能 | 状态 | 评估 |
|------|------|------|
| 对话页 (crob/chat) | ⚠️ | 存在但仅支持 CHAT 场景。不支持 MARKET_ANALYSIS/PROFIT_MODEL 的结构化输入 |
| crob API 模块 | ❌ | **完全缺失**：createTask/sse/fetch 逻辑内联在 vue 组件中 |
| 任务列表页 | ❌ | **缺失** |
| 任务详情页 | ❌ | **缺失** |
| attempt 时间线 | ❌ | **缺失** |
| 报告渲染页 | ❌ | **缺失** |
| 配置管理页 | ❌ | **缺失**（provider/model/policy/pricing 均无） |

### 3.2 路由与权限

| 功能 | 状态 |
|------|------|
| /crob/chat 路由 | ✅ 已在 remaining.ts 注册 |
| 权限白名单 | ✅ /crob/chat 在白名单 |
| 其他 crob 路由 | ❌ 缺失 |

### 3.3 芋道痕迹（需清除 — 严重）

| 位置 | 说明 |
|------|------|
| package.json | name: `yudao-ui-admin-vue3`，repo: gitee yudaocode |
| .env / .env.dev / .env.prod | VITE_APP_TITLE=芋道管理系统，URL 指向 yudao.iocoder.cn |
| index.html | meta 标签含"芋道管理系统" |
| README.md | 全文芋道 branding |
| 各 vue 页面 | doc.iocoder.cn 链接（如 system/user, infra/webSocket 等） |
| vite.config.ts | 注释引用 gitee yudaocode issues |
| permission.ts | 注释引用 gitee yudaocode issues |
| src/config/axios/service.ts | doc.iocoder.cn 链接 |
| src/store/modules/tagsView.ts | 注释引用 gitee yudaocode issues |

---

## 四、部署 (deploy/)

| 功能 | 状态 | 评估 |
|------|------|------|
| docker-compose.yml | 待确认 | 文件存在但需验证 |
| Postgres 初始化 SQL | ✅ | 5 个文件，crob 6 张表完整 |
| Nginx 反代 | ✅ | SSE 关闭缓冲，/internal 禁止透出 |
| .env / .env.example | 待确认 | 需验证 |
| HTTP 测试用例 | ✅ | 5 个 JSON 文件 |

---

## 五、决策记录

| Decision | Rationale |
|----------|-----------|
| Java package: `com.crob.agent` | 用户确认，避免芋道痕迹 |
| 前端保留完整管理后台功能 | 用户要求：租户/部门/用户/登录/菜单权限 |
| 借鉴芋道但不照抄 | 商用需要，去除 yudao/iocoder/ruoyi 所有品牌痕迹 |
| MVP 修复顺序：Java → Python → 前端 | 先修复 Java 控制面（状态权威），再对齐 Python，最后前端 |

## Resources
- 产品方案: `docs/选品场景-AI-Agent产品方案-统一版.md`
- 技术方案: `docs/技术方案-选品Agent任务平台-v2-yudao+pg.md`
- 开发任务清单: `docs/开发任务清单-v2-yudao+pg.md`
- 契约: `docs/附录-A_契约与接口.md`
- MVP 清单: `docs/附录-B_MVP落地清单.md`
- 芋道源码: https://gitee.com/zhijiantianya/yudao-cloud

---
*Update this file after every 2 view/browser/search operations*
