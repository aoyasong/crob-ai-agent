# Findings & Decisions v2

> 基于 `docs/选品场景-AI-Agent产品方案-final.md` 产品方案（尤其是第 11 章 MVP 交付项）与 `docs/技术方案-选品Agent任务平台-v2-yudao+pg.md` 技术方案，重新评估现状差距。
>
> 本版修正了 v1 的 4 处事实错误、2 处严重度误判、3 处 MVP 范围偏差，并补充了 4 处遗漏的差异点。

---

## MVP 范围（来自产品方案第 11 章）

**做什么（双柱落地）：** 市场分析（基础版）+ 利润测算（简化版）
**Guardrails：** L1 意图分类 + L3 输入校验 + L4 数据来源校验（基础版） + 100% 人工抽检
**不做什么：** 竞品监控、选品推荐、自动化质量评估（L7）、L2 时效性校验（MVP 后）、L5 完整溯源（MVP 后）、L6 输出生成约束（MVP 后）

> **关键参考**：产品方案 11.2.2 节 MVP 交付项清单（行 4307-4311）是评估现状的权威基准。
> L2/L5/L6 属于 V1.0（Week 5-12），L7 自动化质量审核属于 V1.5（Week 13-24）。

---

## 一、Java 控制面 (java-control/)

### 1.1 Crob 业务模块

| 功能 | 状态 | 评估 | 说明 |
|------|------|------|------|
| POST /api/tasks（创建+Idempotency-Key） | ✅ | 完整 | 缺参→WAITING_INPUT，门禁失败→REJECTED，完整→QUEUED |
| POST /api/tasks/{id}/inputs（补参） | ✅ | 完整 | merge inputs，再次校验，仅 WAITING_INPUT 可补参 |
| GET /api/tasks/{id} | ✅ | 完整 | 返回聚合状态+disposition+missingFields+reportUrl |
| GET /api/tasks/{id}/attempts | ✅ | 完整 | 按 attempt_id 降序 |
| POST /api/tasks/{id}/cancel | ⚠️ | 基本可用 | 设置 Redis cancel 标志+DB 状态。**缺 CANCELLING 中间态，直接 CANCELLED** |
| POST /api/tasks/{id}/rerun | ✅ | 完整 | 新 attempt + Idempotency-Key + override_constraints |
| GET /api/tasks/{id}/stream（SSE） | ✅ | 可用 | 使用 ReadOffset.from("0-0") 从头读取，不会丢失历史 delta。**v1 文档此处有误，已修正** |
| GET /tasks/{id}/report | ⚠️ | 基本可用 | Markdown HTML 转义渲染。**但仅 escape，无 Markdown→HTML 解析** |
| GET /api/tasks/{id}/artifacts/{id} | ❌ | **缺失** | 无 artifact 代理下载端点（技术方案 3.1 节要求） |
| 超时扫描 | ✅ | 已实现 | `@Scheduled(fixedRate = 30000) scanTimeoutAttempts()`，5 分钟超时→E_TIMEOUT。**v1 文档此处有误，已修正** |
| RestTemplate Bean | ❓ | 待确认 | CrobExecutorClientImpl 注入 RestTemplate，需确认 Bean 定义是否存在 |
| usage_event cost_fen 回填 | ❌ | **缺失** | usage_event 记录后未根据 pricing 表回填 cost |

### 1.2 内部事件处理

| 功能 | 状态 | 评估 |
|------|------|------|
| POST /internal/task-events | ✅ | 完整：attempt_event + usage_event 双通道 |
| 事件幂等（task_id+attempt_id+seq） | ✅ | DuplicateKeyException 兜底 |
| 状态迁移校验 | ✅ | ATTEMPT_STARTED→RUNNING, SUCCEEDED/FAILED/CANCELLED |
| HEARTBEAT 处理 | ✅ | 更新 last_heartbeat_at。**v1 文档此处有误，已修正** |

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
| crob_artifact | ✅ | ✅ | ✅ | SQL/DO/Mapper 均已补齐 |

以下表来自**技术方案**（非产品方案 MVP 范围），产品方案未要求 MVP 阶段实现：

| 表 | 状态 | 所属阶段 |
|----|------|----------|
| provider_config | ❌ | 技术方案要求，MVP 非必需 |
| model_catalog | ❌ | 技术方案要求，MVP 非必需 |
| pricing | ❌ | 技术方案要求，MVP 非必需（但 cost_fen 回填依赖此表） |
| policy_template | ❌ | 技术方案要求，V1.0+ 需要 |
| scenario_default_policy | ❌ | 技术方案要求，V1.0+ 需要 |

### 1.5 产品方案 MVP 交付项逐项对齐

根据产品方案第 11 章（行 4307-4311）MVP 交付项：

| MVP 交付项 | 状态 | 差距 |
|-----------|------|------|
| 市场分析（基础版）— 单品类/3 平台/5 市场/简化模型 | ⚠️ | Java 侧门禁+调度完整，实际分析在 Python 执行 |
| 利润测算（简化版）— 核心成本项/静态测算/单 SKU | ⚠️ | Java 侧门禁+调度完整，实际计算在 Python 执行 |
| L1 意图分类（核心圈/扩展圈/红线） | ⚠️ | Java 侧只有场景路由+门禁校验，意图分类/L1 判定在 Python |
| L3 输入校验 — 参数合法性（范围/格式） | ⚠️ | Java 只做了必填字段检查+OTHER 拒绝，**缺少数值范围校验（如 cost_fen>0、price_fen>0）** |
| L4 数据来源校验（Tier 1/2/3 分级） | ⚠️ | Java 侧无 Tier 分级逻辑，数据源可信度依赖 Python 标注 |
| 100% 人工抽检机制 | ❌ | 无抽检流程/记录存储/质量问题模板 |

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
| MARKET_ANALYSIS 场景 | ⚠️ | 5 步流程（MA_01~MA_05）已定义 STEP_CHANGED 事件。**但缺少产品方案要求的"3 平台×5 市场"参数校验、容量估算方法论透明标注** |
| PROFIT_MODEL 场景 | ⚠️ | 6 步流程（PM_01~PM_06）已定义。**但直接硬编码 cost_fen - price_fen 计算，缺少产品方案要求的费率数据（平台佣金/FBA/物流）加载与标注** |
| CHAT 场景 | ⚠️ | 可用，但 CHAT 不在产品方案定义的四个核心场景中。属于辅助模式，产品方案未定义其边界 |
| Redis Stream 流式输出 | ✅ | xadd delta 到 stream |
| HMAC 事件回调 Java | ⚠️ | java_client.py 实现。**但 `_post_attempt_event` 不等待 HTTP 响应（fire-and-forget），可能导致事件丢失** |
| cancel 检查 | ✅ | `_check_cancel` 在场景入口和 LLM 流式 chunk 之间都有检查 |
| HEARTBEAT 事件 | ✅ | daemon 线程每 30s 发送。**v1 文档此处有误，已修正** |
| USAGE_REPORTED 事件 | ✅ | LLM 调用后上报 token 用量。**v1 文档此处有误，已修正** |
| attempt 锁 | ✅ | Redis SET NX + TTL 1800s |
| task_result 落库 | ✅ | upsert 到 crob_task_result |
| L1 边界判断 | ⚠️ | `_l1_check` 实现 scenario 白名单+红线关键词。**v1 文档标 ❌ 过严**。但缺少产品方案要求的"核心圈/扩展圈"意图分类（仅做了红线拒绝） |
| L4 真实性校验 | ⚠️ | MARKET_ANALYSIS 有基础来源标注（source/observed_at/confidence）。**v1 文档标 ❌ 过严**。但缺少产品方案要求的 Tier 1/2/3 分级和交叉验证 |
| L5 溯源包装 | ❌ | **属于 V1.0 范围**，MVP 不做。当前 MARKET_ANALYSIS 有初步 data_point_id，但未形成完整溯源链 |
| 错误码映射 | ⚠️ | 部分：E_SCOPE_REDLINE, E_SCOPE_UNSUPPORTED, E_DATA_UNAVAILABLE, E_INPUT_INVALID, E_CANCELLED, E_EXECUTOR_FAILED |

### 2.2 产品方案 MVP 交付项逐项对齐

| MVP 交付项 | 状态 | 差距 |
|-----------|------|------|
| 市场分析：数据来源标注率 100% | ⚠️ | MARKET_ANALYSIS 有来源标注，但 PROFIT_MODEL 和 CHAT 场景**完全没有来源标注** |
| 市场分析：容量估算偏差 < 50% | ❓ | 未测量，当前依赖静态 JSON 数据集，无实时估算模型 |
| 利润测算：公式透明可验证 | ❌ | 当前硬编码计算，无公式展示/费率标注/计算过程透明 |
| 利润测算：费率数据标注有效期 | ❌ | 未加载费率数据，更未标注有效期 |
| 利润测算：测算偏差 < 30% | ❓ | 未测量 |
| L3 输入校验 — 数值范围/格式 | ❌ | Python 侧未做 cost_fen > 0、price_fen > 0 等数值合法性校验 |
| L4 数据来源校验（Tier 1/2/3） | ❌ | 无 Tier 分级体系，仅有简单的 source 字段标注 |

### 2.3 Python 代码质量

- ✅ HMAC 实现与 Java 完全一致（sha256 + canonical 格式）
- ✅ 无第三方框架依赖（仅 FastAPI + httpx + redis + psycopg）
- ✅ 包结构清晰，无芋道痕迹
- ⚠️ `_post_attempt_event` fire-and-forget，可能导致事件丢失
- ⚠️ `stream_chat_completions` 内部无 cancel 检查（外层 `_llm_generate_report` 逐 chunk 检查，实际影响有限）

---

## 三、前端 (web-admin/crob-admin-web/)

### 3.1 Crob 业务页面

| 功能 | 状态 | 评估 |
|------|------|------|
| 对话页 (crob/chat) | ⚠️ | 存在但仅支持 CHAT 场景。**不支持 MARKET_ANALYSIS/PROFIT_MODEL 的结构化输入**（产品方案要求市场分析输入：品类/平台/市场；利润测算输入：成本/售价/物流/平台/市场） |
| crob API 模块 | ❌ | **完全缺失**：createTask/sse/fetch 逻辑内联在 vue 组件中 |
| 任务列表页 | ❌ | **缺失**（种子用户白手套服务可暂缓，但 V1.0 必须） |
| 任务详情页 | ❌ | **缺失** |
| attempt 时间线 | ❌ | **缺失** |
| 报告渲染页 | ❌ | **缺失**（产品方案 MVP 要求展示报告） |
| 配置管理页 | ❌ | **缺失**（技术方案要求，MVP 非必需） |

### 3.2 路由与权限

| 功能 | 状态 |
|------|------|
| /crob/chat 路由 | ✅ 已在 remaining.ts 注册 |
| 权限白名单 | ✅ /crob/chat 在白名单 |
| 其他 crob 路由 | ❌ 缺失 |

### 3.3 产品方案 MVP 前端差距

产品方案 MVP 阶段的核心交互是"对话式问答"，种子用户通过白手套服务使用。当前前端仅有一个通用 CHAT 对话页，**缺少两个 MVP 场景的结构化输入表单**：

- MARKET_ANALYSIS：需要平台选择器（亚马逊/Temu/TikTok Shop）、市场选择器（美/英/德/日/东南亚）、品类/关键词输入
- PROFIT_MODEL：需要成本、售价、物流方式、平台、市场等结构化参数输入

### 3.4 芋道痕迹（需清除 — 严重）

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
| docker-compose.yml | ✅ | 4 个服务 + 1 个初始化任务，external network 模式 |
| Postgres 初始化 SQL | ✅ | 5 个文件，crob 6 张表完整 |
| Nginx 反代 | ✅ | SSE 关闭缓冲，/internal 禁止透出 |
| .env / .env.example | ✅ | 存在 |
| HTTP 测试用例 | ✅ | 5 个 JSON 文件 |

---

## 五、产品方案覆盖度评估（新增）

以下为产品方案明确要求但 findings v1 未覆盖的维度：

| 产品方案要求 | MVP 是否必需 | 当前状态 | 差距 |
|-------------|-------------|---------|------|
| **不确定性表达体系**（L0-L4 置信度标签、区间表达、来源透明） | ✅ MVP 基础要求 | ❌ | 当前无结构化不确定性标注，LLM prompt 有"不要编造"但未约束输出格式 |
| **输出数据来源标注率 100%** | ✅ MVP L4 交付标准 | ⚠️ | 仅 MARKET_ANALYSIS 有来源标注，其余场景无 |
| **利润测算公式透明可验证** | ✅ MVP 要求 | ❌ | 当前硬编码计算，无公式展示 |
| **费率数据标注有效期** | ✅ MVP 利润测算要求 | ❌ | 未加载费率数据 |
| **L3 数值范围校验**（cost_fen>0、price_fen>0） | ✅ MVP 要求 | ❌ | Java/Python 均未校验 |
| **数据源 Tier 分级体系**（Tier 1/2/3/Prohibited） | ✅ MVP L4 要求 | ❌ | 无 Tier 分级 |
| **禁止模糊表达**（"大概""可能""据说"） | ⚠️ V1.0 | ❌ | 未在 prompt 或 L7 中约束 |
| **用户质疑→溯源展示** | ⚠️ V1.0 | ❌ | 无此机制 |

---

## 六、CHAT 场景的定位问题（新增）

产品方案定义的四个核心场景是：市场分析、竞品监控、利润测算、选品推荐。**CHAT 不在其中**。当前代码将 CHAT 作为与 MARKET_ANALYSIS/PROFIT_MODEL 并列的 scenario，但产品方案未定义 CHAT 的：

- 边界（什么问题可以聊？）
- Guardrails 要求（是否需要 L1-L7？）
- 质量评估标准

**建议**：产品方案应补充 CHAT 场景的边界定义，或明确 MVP 阶段 CHAT 仅用作开发调试、不对用户开放。

---

## 七、决策记录

| Decision | Rationale |
|----------|-----------|
| Java package: `com.crob.agent` | 用户确认，避免芋道痕迹 |
| 前端保留完整管理后台功能 | 用户要求：租户/部门/用户/登录/菜单权限 |
| 借鉴芋道但不照抄 | 商用需要，去除 yudao/iocoder/ruoyi 所有品牌痕迹 |
| MVP 修复顺序：Java → Python → 前端 | 先修复 Java 控制面（状态权威），再对齐 Python，最后前端 |
| MVP Guardrails 范围严格按产品方案第 11 章 | L1+L3+L4 基础版 + 100% 人工抽检。L2/L5/L6→V1.0，L7 自动化→V1.5 |
| 产品方案 vs 技术方案 区分评估 | provider/model/pricing/policy 等技术方案要求的表，不与产品方案 MVP 交付项混为一谈 |

---

## v2 较 v1 修正清单

| v1 原文 | 修正 | 原因 |
|---------|------|------|
| HEARTBEAT ❌ 缺失 | ✅ 已实现 | executor.py:32-41 有 daemon 线程发送 |
| USAGE_REPORTED ❌ 缺失 | ✅ 已实现 | executor.py:47 调用了 _post_usage_event |
| 超时扫描 ❌ 缺失 | ✅ 已实现 | CrobTaskServiceImpl.java:575-602 @Scheduled |
| SSE readOffset.latest() 丢历史 | ✅ 使用 from("0-0") | CrobTaskServiceImpl.java:426 |
| L1 边界判断 ❌ 缺失 | ⚠️ 部分完成 | _l1_check 有 scenario 白名单+红线关键词 |
| L4 真实性校验 ❌ 缺失 | ⚠️ 部分完成 | MARKET_ANALYSIS 有基础来源标注 |
| MVP Guardrails: L1/L3/L4/L5 | L1+L3+L4（L5 是 V1.0） | 按产品方案第 11 章修正 |
| L7 人工抽检 | 100% 人工抽检（与 L7 质量审核不同） | L7 是 V1.5 自动化质量审核 |
| crob_artifact ❌ | ✅ | DO/Mapper 均已补齐 |

---

## Resources

- 产品方案: `docs/选品场景-AI-Agent产品方案-final.md`
- 技术方案: `docs/技术方案-选品Agent任务平台-v2-yudao+pg.md`
- 开发任务清单: `docs/开发任务清单-v2-yudao+pg.md`
- 契约: `docs/附录-A_契约与接口.md`
- MVP 清单: `docs/附录-B_MVP落地清单.md`

---

*最后更新：2026-06-02*
