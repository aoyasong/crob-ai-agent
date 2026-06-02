# Harness Engineering 标准迭代升级计划

> 基于《选品场景-AI-Agent产品方案-final.md》的 Harness Engineering 方法论，对照当前实现状态的差距分析及技术方案。

---

## 0. 当前基准

| 维度 | 完成度 | 关键资产 |
|------|--------|---------|
| 边界控制 L1 | 20% | `_l1_check()` 关键词匹配 6 个红线词 |
| 安全护栏 L1-L7 | 35% | L1(基础)/L4(静态标注)/L5(仅MA场景)/L6(可用) |
| 工作流编排 | 15% | 线性 if-else，STEP_CHANGED 事件 |
| 多Agent协作 | 0% | 单一 executor.py |
| 权限控制 | 30% | super_admin / common 角色 |
| 可观测性 | 20% | crob_task_event / crob_usage_event 表 |
| 评估体系 | 0% | 无 |

---

## 1. MVP 补全（P0 — 当前阶段）

> 目标：补齐文档 MVP 清单中"应有但未实现"的能力，达到 MVP 发布门禁。

### 1.1 L3 输入校验强化

**当前**：Java 端做字段级 `@NotBlank` 校验；Python 端无输入安全校验。

**方案**：在 Python executor `_l1_check` 之后增加 `_l3_check` 函数。

```python
# python-executor/app/executor.py 新增

def _l3_check(task_id, attempt_id, seq, scenario, inputs):
    """L3 input validation: completeness, legality, injection defense."""
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "L3_VALIDATE_INPUT"})

    issues = []

    # 1. SQL/Command injection pattern check
    injection_patterns = [
        r"(?i)(drop\s+table|delete\s+from|exec\s*\(|system\s*\()",
        r"(?i)(<script|javascript:|onerror\s*=)",
    ]
    for key, val in inputs.items():
        if isinstance(val, str):
            for pat in injection_patterns:
                if re.search(pat, val):
                    issues.append({"field": key, "reason": "potential injection"})

    # 2. Length limits
    for key, val in inputs.items():
        if isinstance(val, str) and len(val) > 4000:
            issues.append({"field": key, "reason": "exceeds max length"})

    # 3. Valid enum values
    valid_enums = {
        "platform": {"AMAZON", "TEMU", "TIKTOK_SHOP"},
        "market": {"US", "UK", "DE", "FR", "JP"},
        "shipping_mode": {"FBA", "FBM"},
    }
    for key, valid_set in valid_enums.items():
        if key in inputs and str(inputs[key]).upper() not in valid_set:
            issues.append({"field": key, "reason": f"must be one of {valid_set}"})

    if issues:
        _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED",
                           {"step": "L3_REJECTED", "issues": issues})
        raise RuntimeError(f"E_INPUT_INVALID: {json.dumps(issues, ensure_ascii=False)}")

    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "L3_PASSED"})
```

**涉及文件**：`python-executor/app/executor.py`

---

### 1.2 L5 溯源包装全覆盖

**当前**：仅 `_run_market_analysis` 有溯源字段，CHAT 和 PROFIT_MODEL 缺失。

**方案**：将溯源包装抽取为公共函数，所有场景统一调用。

```python
# python-executor/app/executor.py 新增

def _l5_provenance(task_id, attempt_id, seq, source_type: str, meta: dict) -> dict:
    """L5 provenance wrapper — attach source metadata to every data point."""
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED",
                       {"step": "L5_PROVENANCE"})

    provenance = {
        "source_type": source_type,        # "dataset" | "llm_knowledge" | "user_input"
        "source_id": meta.get("source_id", "unknown"),
        "observed_at": meta.get("observed_at", datetime.utcnow().isoformat()),
        "confidence": meta.get("confidence", 0.5),
        "limitations": meta.get("limitations", []),
        "retrieved_at": datetime.utcnow().isoformat(),
    }
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED",
                       {"step": "L5_PROVENANCE", "provenance": provenance})
    return provenance
```

各场景调用点：

| 场景 | source_type | 来源 |
|------|------------|------|
| MARKET_ANALYSIS | `dataset` | JSON 数据文件路径 |
| PROFIT_MODEL | `user_input` | 用户提供的成本/售价 |
| CHAT | `llm_knowledge` | LLM 内置知识 + 免责声明 |

**涉及文件**：`python-executor/app/executor.py`

---

### 1.3 L7 基础质量审核

**当前**：无输出审核，LLM 输出直接返回用户。

**方案**：在 `_llm_generate_report` 之后增加 `_l7_review` 函数，做规则级别的输出检查。

```python
# python-executor/app/executor.py 新增

def _l7_review(task_id, attempt_id, seq, report_md: str, scenario: str) -> dict:
    """L7 quality review: rule-based output audit before delivery."""
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "L7_REVIEW"})

    flags = []

    # 1. 幻觉标记检测：出现"数据显示""根据最新"但没有引用来源
    citation_phrases = ["数据显示", "根据.*统计", "最新数据表明", "研究表明"]
    has_citation_hint = any(re.search(p, report_md) for p in citation_phrases)
    has_source = bool(re.search(r"来源[：:]|数据来源|source:", report_md))
    if has_citation_hint and not has_source:
        flags.append({"severity": "ERROR", "reason": "引用性表述缺少来源标注",
                       "rule": "citation_without_source"})

    # 2. 编造风险：出现精确数字但没有来源
    numbers_without_source = re.findall(r"\d+(?:\.\d+)?%", report_md)
    if len(numbers_without_source) > 3 and not has_source:
        flags.append({"severity": "WARN", "reason": "多处精确数字但无来源",
                       "rule": "numbers_without_source"})

    # 3. 红线词残留检查
    redline = ["投资建议", "保证收益", "稳赚", "零风险"]
    for word in redline:
        if word in report_md:
            flags.append({"severity": "BLOCK", "reason": f"包含红线词: {word}",
                           "rule": "redline_in_output"})

    # 4. 最小长度检查
    if len(report_md.strip()) < 50:
        flags.append({"severity": "WARN", "reason": "输出过短，可能不完整",
                       "rule": "output_too_short"})

    blocks = [f for f in flags if f["severity"] == "BLOCK"]
    if blocks:
        _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED",
                           {"step": "L7_BLOCKED", "flags": flags})
        # 返回保守模板化回复
        return {"passed": False, "flags": flags,
                "fallback": f"抱歉，当前无法生成满足质量标准的{scenario}报告。"
                             "请尝试调整查询条件后重试。"}

    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED",
                       {"step": "L7_PASSED", "flags": flags})
    return {"passed": True, "flags": flags}
```

**涉及文件**：`python-executor/app/executor.py`

---

### 1.4 MVP 补全后的 L1-L7 覆盖

```
L1 ✅ 边界判断（关键词 + 场景枚举）
L2 ❌ 跳过（MVP 数据源为静态文件，无需时效性校验）
L3 ✅ 输入校验（注入检测 + 长度限制 + 枚举校验）
L4 -  静态数据源标注、降级声明（已有基础）
L5 ✅ 溯源包装（所有场景统一输出来源标注）
L6 ✅ 输出生成（LLM 调用）
L7 ✅ 质量审核（规则级输出检查）
```

---

## 2. V1.0 生产级（P1 — 下个里程碑）

> 目标：从"能用"到"好用"，核心指标达到 50% 综合质量。

### 2.1 L1 边界升级：从关键词到意图分类器

**方案**：用 LLM 做小样本意图分类，替代关键词匹配。

**实现**：在 `_l1_check` 中增加 LLM 调用分支。

```python
# 意图分类 prompt 模板
INTENT_CLASSIFIER_PROMPT = """判断以下用户输入是否属于跨境电商选品领域。

输出 JSON：
{
  "in_scope": true/false,
  "category": "核心圈|扩展圈|红线",
  "reason": "简短原因"
}

核心圈：市场分析、竞品调研、利润测算、选品建议、供应链评估
扩展圈：平台政策咨询、物流方案、支付方式（可回答但需标注能力边界）
红线：投资建议、法律意见、税务规避、灰色清关、仿牌

用户输入：{user_input}
"""
```

**涉及文件**：`python-executor/app/executor.py`

---

### 2.2 L4 真实性校验升级

**方案**：引入数据源分级（Tier 1/2/3/Prohibited）+ 最小交叉验证。

```python
DATA_SOURCE_TIERS = {
    "dataset": 1,          # Tier 1: 内部审定数据
    "public_api": 2,        # Tier 2: 公开API
    "llm_knowledge": 3,     # Tier 3: 仅参考
    "unauthorized": 99,     # Prohibited
}

def _l4_truth_verify(source_type: str, data_point: dict) -> dict:
    """Cross-validate data point against source tier rules."""
    tier = DATA_SOURCE_TIERS.get(source_type, 3)

    if tier == 99:
        raise RuntimeError("E_PROHIBITED_SOURCE")

    confidence_map = {1: 0.85, 2: 0.60, 3: 0.35}
    return {
        "tier": tier,
        "confidence": confidence_map[tier],
        "requires_disclaimer": tier >= 2,
        "fallback_allowed": tier >= 3,
    }
```

---

### 2.3 可观测性 L2 业务面板

**方案**：基于已有的 `crob_task_event` 和 `crob_usage_event` 表构建业务监控。

**Java 后端新增**：

```java
// 新增 DashboardController: /api/system/dashboard/task-stats
@GetMapping("/task-stats")
public CommonResult<Map<String, Object>> taskStats(
        @RequestParam(defaultValue = "7") int days) {
    // 统计指标：
    // - 任务总量 / 成功率 / 失败率
    // - 各场景分布
    // - 平均处理时长
    // - P50 / P95 / P99 延迟
    // - 今日消费金额 / token 使用量
}
```

**涉及文件**：新增 `DashboardController.java`、`DashboardService.java`

---

### 2.4 人工抽检流程（Java 后端）

**方案**：新增 Review 模块，支持对每一条输出进行人工复核。

**数据表**：

```sql
CREATE TABLE IF NOT EXISTS crob_review (
    review_id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    reviewer_id BIGINT,
    status VARCHAR(32) DEFAULT 'PENDING',  -- PENDING / PASSED / FAILED / DISPUTED
    score_accuracy INT,          -- 准确性 1-5
    score_completeness INT,      -- 完整性 1-5
    score_relevance INT,         -- 相关性 1-5
    flags_json TEXT,             -- 问题标记
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**涉及文件**：新增 `ReviewController.java`、`CrobReviewDO.java`、`CrobReviewMapper.java`

---

## 3. V1.5 规模化（P2）

### 3.1 工作流 DAG 编排

**方案**：将 Python executor 从线性 if-else 升级为 DAG 执行器。

**核心设计**：

```python
# python-executor/app/dag_executor.py 新增

from dataclasses import dataclass
from typing import List, Dict, Callable

@dataclass
class Step:
    id: str
    name: str
    handler: Callable
    depends_on: List[str]       # 前置步骤 ID 列表
    can_parallel: bool = False  # 是否可与其他步骤并行
    max_retries: int = 0        # 最大重试次数
    timeout_seconds: int = 60

class DagExecutor:
    """DAG-based step executor with topological sort."""
    def __init__(self, steps: List[Step]):
        self.steps = {s.id: s for s in steps}
        self._validate_dag()  # 检测循环依赖

    def execute(self, context: dict) -> dict:
        """按拓扑顺序执行，无依赖的并行执行。"""
        ready_queue = [s for s in self.steps.values() if not s.depends_on]
        results = {}

        while ready_queue:
            # 并行组：当前所有就绪步骤一起执行
            parallel_group = [s for s in ready_queue if s.can_parallel]
            sequential = [s for s in ready_queue if not s.can_parallel]

            if parallel_group:
                with ThreadPoolExecutor(max_workers=len(parallel_group)) as pool:
                    futures = {pool.submit(s.handler, context): s for s in parallel_group}
                    for f in as_completed(futures):
                        results[futures[f].id] = f.result()

            for step in sequential:
                results[step.id] = step.handler(context)

            # 找到下一批就绪步骤
            ready_queue = [
                s for s in self.steps.values()
                if s.id not in results
                and all(d in results for d in s.depends_on)
            ]

        return results
```

**场景 DAG 定义示例 — MARKET_ANALYSIS**：

```python
MARKET_ANALYSIS_DAG = [
    Step("l1_check",     "边界判断",       _l1_handler,     depends_on=[]),
    Step("l3_check",     "输入校验",       _l3_handler,     depends_on=["l1_check"]),
    Step("fetch_data",   "获取数据",       _fetch_handler,  depends_on=["l3_check"]),
    Step("l4_verify",    "真实性校验",     _l4_handler,     depends_on=["fetch_data"]),
    Step("l5_provenance","溯源包装",       _l5_handler,     depends_on=["l4_verify"]),
    Step("l6_generate",  "LLM生成",        _l6_handler,     depends_on=["l5_provenance"]),
    Step("l7_review",    "质量审核",       _l7_handler,     depends_on=["l6_generate"]),
]
```

**涉及文件**：新增 `python-executor/app/dag_executor.py`

---

### 3.2 Explore / Plan / Act 状态分离

**方案**：在 Java 的任务状态机中增加 EPA 三态。

```
     ┌──────────┐      ┌──────────┐      ┌──────────┐
     │ EXPLORE  │ ───→ │   PLAN   │ ───→ │   ACT    │
     │ (只读)   │      │ (规划)   │      │ (执行)   │
     └──────────┘      └──────────┘      └──────────┘
          │                  │                  │
    允许：搜索/查询      允许：校验/分析    允许：写入/生成
    禁止：写入/调用      禁止：外部修改      需：二次确认
```

**Java 状态机扩展**：

```java
// CrobTaskServiceImpl 新增状态
private static final String STATUS_EXPLORING = "EXPLORING";
private static final String STATUS_PLANNING = "PLANNING";
// STATUS_RUNNING → STATUS_ACTING (rename for clarity)
```

**涉及文件**：`CrobTaskServiceImpl.java`、`executor.py`

---

### 3.3 L2 时效性校验

**方案**：数据文件增加有效期字段，Python 端在读取前校验。

```python
def _l2_timeliness_check(data_file_meta: dict) -> dict:
    """Check if data is still fresh enough for use."""
    observed_at = data_file_meta.get("observed_at")
    max_age_days = data_file_meta.get("max_age_days", 30)

    age = (datetime.utcnow() - parse_date(observed_at)).days
    if age > max_age_days:
        return {
            "fresh": False,
            "age_days": age,
            "action": "WARN",  # 标注"数据可能过时"
            "disclaimer": f"数据采集于 {observed_at}，已超过 {max_age_days} 天"
        }
    return {"fresh": True, "age_days": age}
```

---

## 4. V2.0 质量达标（P3）

### 4.1 Prompt 模板管理 — 多Agent 工作空间系统

> 参照 OpenClaw 的 workspace 设计哲学：每个 Agent 拥有独立的工作空间，通过文件系统管理 Prompt 文档，实现"行为定义文档化、上下文装配自动化、提示词版本化"。

#### 4.1.1 设计原则

| 原则 | 说明 |
|------|------|
| **文件即配置** | Agent 的行为、人格、规则、工具全部由 Markdown 文件定义，不硬编码在代码中 |
| **按需装配** | 根据当前场景/角色/任务动态装配上下文，避免把所有规则塞进单次 Prompt |
| **越近越优先** | 场景级规则 > Agent 级规则 > 项目级规则，冲突时向保守策略倾斜 |
| **可审计** | 每次装配记录装配清单（哪些片段被加载、版本、时间） |
| **可版本化** | 所有 Prompt 文件纳入 Git，支持 diff、回滚、A/B 测试 |

#### 4.1.2 工作空间目录结构

每个 Agent 的工作空间是一个独立的文件目录，存放在 `python-executor/workspaces/{agent_id}/`：

```
python-executor/workspaces/
├── _shared/                          # 共享规则（全局继承）
│   ├── GLOBAL_RULES.md               # 全局铁律：红线、真实性原则、输出风格
│   ├── TERMINOLOGY.md                # 统一术语表
│   └── DATA_SOURCE_TIERS.md          # 数据源分级规范
│
├── orchestrator/                     # 主控 Agent 工作空间
│   ├── SOUL.md                       # 人格定义
│   ├── AGENTS.md                     # 决策逻辑：任务分解、Agent 调度、结果合并
│   ├── TOOLS.md                      # 可用工具：scenario 路由、Context Store 读写
│   ├── MEMORY.md                     # 长期记忆：调度经验、常见任务模式
│   ├── prompts/                      # 场景化 Prompt 模板
│   │   ├── task_decomposition.md     # 任务分解模板
│   │   ├── result_merge.md           # 多Agent 结果合并模板
│   │   └── fallback.md               # 降级策略模板
│   └── checklists/                   # 操作检查清单
│       └── pre_dispatch.md           # 派发前检查
│
├── market_analyst/                   # 市场分析 Agent 工作空间
│   ├── SOUL.md                       # "资深跨境电商市场分析师"
│   ├── AGENTS.md                     # 分析框架：市场规模→趋势→竞品→机会
│   ├── TOOLS.md                      # 数据查询API、公开数据集、搜索工具
│   ├── KNOWLEDGE.md                  # 领域知识：平台规则、品类特征
│   ├── prompts/
│   │   ├── market_sizing.md          # 市场规模估算模板
│   │   ├── trend_analysis.md         # 趋势分析模板（PESTEL框架）
│   │   ├── competitor_scan.md        # 竞品扫描模板
│   │   ├── opportunity_scoring.md    # 机会评分模板
│   │   └── report_assembly.md        # 报告组装模板
│   ├── checklists/
│   │   ├── pre_analysis.md           # 分析前检查：数据是否完整？来源是否可靠？
│   │   └── quality_gate.md           # 质量门禁：关键结论是否有数据支撑？
│   └── examples/                     # Few-shot 示例（评估集样本）
│       ├── wireless_earbuds_good.md  # 优质输出示例
│       └── wireless_earbuds_bad.md   # 问题输出示例（反面教材）
│
├── competitor_analyst/               # 竞品分析 Agent
│   ├── SOUL.md
│   ├── AGENTS.md
│   ├── TOOLS.md
│   ├── prompts/
│   │   ├── competitor_landscape.md   # 竞争格局分析
│   │   ├── pricing_analysis.md       # 定价策略分析
│   │   ├── review_mining.md          # 评论挖掘模板
│   │   └── swot_template.md          # SWOT 分析模板
│   └── checklists/
│
├── profit_modeler/                   # 利润测算 Agent
│   ├── SOUL.md
│   ├── AGENTS.md
│   ├── TOOLS.md
│   ├── KNOWLEDGE.md                  # 费率知识库：FBA费率、各平台佣金、汇率
│   ├── prompts/
│   │   ├── cost_breakdown.md         # 成本拆解模板
│   │   ├── profit_sensitivity.md     # 敏感性分析模板
│   │   ├── fee_schedule.md           # 费率表模板
│   │   └── roi_projection.md         # ROI 预测模板
│   └── checklists/
│
└── compliance_checker/               # 合规检查 Agent
    ├── SOUL.md
    ├── AGENTS.md
    ├── TOOLS.md
    ├── REDLINE_RULES.md              # 红线规则（独立文件，不可被 override）
    ├── prompts/
    │   ├── ip_check.md               # 知识产权检查
    │   ├── import_regulation.md      # 进口法规检查
    │   └── certification_check.md    # 认证要求检查
    └── checklists/
```

#### 4.1.3 核心 Prompt 文件格式

每个 Prompt 文件采用 **YAML Frontmatter + Markdown 正文** 格式：

**SOUL.md 示例**：

```markdown
---
agent: market_analyst
version: 1.2.0
updated: 2026-06-01
stability: stable         # stable | beta | experimental
temperature: 0.3          # 该 Agent 的推荐 temperature
model: DeepSeek-V3        # 推荐模型
max_tokens: 4096
---

# Market Analyst Agent

## 身份
你是一名资深跨境电商市场分析师，拥有 10 年 Amazon/Temu/TikTok Shop 选品经验。
你的分析风格是 **数据驱动、结构清晰、坦率直接**。

## 核心能力
- 市场规模估算与增长预测
- 品类趋势分析（季节性、生命周期、技术替代）
- 竞争格局扫描（集中度、头部卖家、新进入者）
- 机会评分与风险排序

## 工作原则
1. 所有结论必须有数据来源支撑，不允许出现无来源的数字
2. 数据不足时明确告知用户"当前信息不足以做出判断"，不要猜测
3. 优先使用内部数据集，其次使用公开权威数据，最后使用 LLM 知识（须标注）
4. 给出建议时必须附带"如果…则…"的条件说明

## 输出风格
- 先结论后证据（倒金字塔结构）
- 关键数字用表格呈现
- 不确定性用 `confidence: 0.XX` 标注
- 风险项单独列出并标注严重程度
```

**AGENTS.md 示例**：

```markdown
---
agent: market_analyst
version: 1.1.0
priority_rules:
  - rule: "数据缺失时拒绝分析"
    priority: CRITICAL
    action: DEGRADE
  - rule: "交叉验证阈值"
    priority: HIGH
    params: { min_sources: 2 }
---

# 决策逻辑

## 任务接收
收到主控 Agent 派发的 market_analysis 任务后，按以下流程执行：

1. **输入校验**：检查 platform/market/category_or_keyword 是否完整
2. **数据获取**：按 TOOLS.md 定义的工具获取数据
3. **质量判断**：
   - 如果数据 Tier=1 且 confidence≥0.7 → 直接生成报告
   - 如果数据 Tier=2 → 生成报告但附加置信度警告
   - 如果数据 Tier=3 或无数据 → 触发 DEGRADE，输出框架+缺失声明

## 降级策略
当数据不足以支撑完整分析时：
- 输出：分析框架 + 已获取的数据 + 无法覆盖的维度列表
- 不输出：任何编造的数字或未经证实的趋势判断
- 向主控 Agent 发送 DEGRADE 事件，由主控决定是否切换 Agent 或请求用户补充

## 异常处理
- 外部数据源不可用 → 重试 2 次 → 使用缓存 → 触发降级
- LLM 调用超时 → 输出已生成部分 + 失败说明
- 任务取消信号 → 保存当前状态到 Context Store → 优雅退出
```

#### 4.1.4 Prompt 装配引擎

**装配管线**（每次 Agent 执行时触发）：

```
                    ┌─────────────────────┐
                    │   装配引擎           │
                    │   PromptAssembler   │
                    └─────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ↓                   ↓                   ↓
   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
   │ 静态层        │   │ 场景层        │   │ 运行时层      │
   │ (Static)     │   │ (Scenario)   │   │ (Runtime)    │
   └──────────────┘   └──────────────┘   └──────────────┘
   加载顺序：基础 → 特定                           最后注入

   静态层内容：                    场景层内容：         运行时层内容：
   - _shared/*.md        - agent/prompts/*.md    - 当前任务 inputs
   - agent/SOUL.md       - agent/checklists/*.md - Context Store 中
   - agent/AGENTS.md     - agent/KNOWLEDGE.md     间结果
   - agent/TOOLS.md      - agent/examples/*.md   - 用户反馈历史
```

**装配规则**：

```python
# python-executor/app/prompt_assembler.py

class PromptAssembler:
    """Dynamic prompt assembly engine with inheritance and override."""

    def __init__(self, workspace_root: str):
        self.root = Path(workspace_root)
        self.audit_log: list[AssemblyRecord] = []

    def assemble(self, agent_id: str, scenario: str,
                 runtime_context: dict) -> AssembledPrompt:
        """Assemble the full system prompt for an agent execution."""
        sections = OrderedDict()

        # Layer 1: Static — shared rules, agent identity, behavior
        sections["global_rules"] = self._load_shared("GLOBAL_RULES.md")
        sections["terminology"] = self._load_shared("TERMINOLOGY.md")
        sections["soul"] = self._load_agent_file(agent_id, "SOUL.md")
        sections["agents"] = self._load_agent_file(agent_id, "AGENTS.md")
        sections["tools"] = self._load_agent_file(agent_id, "TOOLS.md")

        # Layer 2: Scenario — knowledge base, templates, examples
        knowledge = self._load_agent_file(agent_id, "KNOWLEDGE.md")
        if knowledge:
            sections["knowledge"] = knowledge
        sections["redline"] = self._load_agent_file(agent_id, "REDLINE_RULES.md")
        sections["scenario_prompt"] = self._load_prompt_template(agent_id, f"{scenario}.md")

        # Layer 3: Runtime — current task context, intermediate results
        sections["runtime"] = self._format_runtime_context(runtime_context)

        # Merge with conflict resolution
        merged = self._merge_sections(sections, agent_id)
        self._audit(agent_id, scenario, sections)

        return AssembledPrompt(
            system_prompt=merged,
            token_count=count_tokens(merged),
            assembly_record=self.audit_log[-1]
        )

    def _merge_sections(self, sections: dict, agent_id: str) -> str:
        """Merge sections with 'closer wins' priority for conflicts."""
        # 优先级：场景模板 > Agent 文件 > 共享规则
        # 任何"会导致越界/幻觉风险提升"的冲突，向保守策略倾斜
        pass
```

#### 4.1.5 模板变量与参数化

Prompt 模板支持 Jinja2 风格的变量替换：

```markdown
<!-- market_analyst/prompts/market_sizing.md -->
---
template: market_sizing
variables:
  - platform        # 平台名称
  - market          # 目标市场
  - category        # 品类关键词
  - data_summary    # 从数据文件提取的摘要
---

## 市场规模分析

**分析对象**: {{ platform }} / {{ market }} / {{ category }}

{% if data_summary.market_size %}
## 已有数据
{{ data_summary.market_size | to_table }}
{% else %}
## 数据缺口
当前缺少 {{ platform }}-{{ market }} 市场的 {{ category }} 品类规模数据。
以下分析基于可获取的最近似数据，confidence 相应降低。
{% endif %}
```

#### 4.1.6 管理 API — Prompt 模板 CRUD

**Java 后端新增 Prompt 管理接口**：

```java
@RestController
@RequestMapping("/api/system/prompt")
public class PromptTemplateController {

    // GET    /api/system/prompt/tree?agent_id=market_analyst
    //        返回文件树（目录结构 + 文件列表）

    // GET    /api/system/prompt/file?agent_id=market_analyst&path=prompts/market_sizing.md
    //        读取单个文件内容（含 frontmatter 解析）

    // PUT    /api/system/prompt/file
    //        保存/更新文件

    // POST   /api/system/prompt/preview
    //        传入 agent_id + scenario + sample_inputs，返回装配后的完整 System Prompt
    //        用于在管理后台预览和调试 Prompt

    // GET    /api/system/prompt/audit?agent_id=market_analyst
    //        查看装配审计日志

    // POST   /api/system/prompt/diff
    //        对比两个版本的差异

    // POST   /api/system/prompt/rollback
    //        回滚到指定版本（通过 Git）
}
```

**前端管理页面**：在"系统管理"下新增"Prompt 管理"菜单，提供：
- 左侧：Agent 列表 + 工作空间文件树
- 右侧：Markdown 编辑器（支持 frontmatter 可视化编辑 + 预览）
- 顶部：变量输入框 → 点击"预览装配" → 渲染完整 System Prompt
- 底部：版本历史 + diff 对比

#### 4.1.7 版本控制与 A/B 测试

```
工作空间 Git 仓库结构：

workspaces/
├── main/          ← 当前生产分支
├── staging/       ← 预发布分支（灰度）
└── experiments/   ← 实验分支
    ├── exp-001-lower-temperature/
    ├── exp-002-structured-output/
    └── exp-003-shorter-context/

A/B 测试流程：
1. 从 main 创建实验分支 → 修改 Prompt 文件
2. 配置 traffic split: 90% main / 10% experiment
3. 收集评估指标（准确性、用户满意度）
4. 达标 → 合并到 main；不达标 → 废弃分支
```

#### 4.1.8 新增文件清单

```
python-executor/
├── workspaces/                       # Agent 工作空间根目录
│   ├── _shared/                      # 全局共享
│   ├── orchestrator/                 # 主控
│   ├── market_analyst/               # 市场分析
│   ├── competitor_analyst/           # 竞品分析
│   ├── profit_modeler/               # 利润测算
│   └── compliance_checker/           # 合规检查
├── app/
│   ├── prompt_assembler.py           # 装配引擎
│   ├── prompt_loader.py              # 文件加载 + YAML frontmatter 解析
│   └── prompt_audit.py               # 装配审计日志

java-control/
├── (新增) .../controller/admin/prompt/PromptTemplateController.java
├── (新增) .../service/prompt/PromptTemplateService.java
├── (新增) .../service/prompt/PromptAssemblerService.java  # 调用 Python 装配预览

web-admin/
├── (新增) src/views/system/prompt/index.vue   # Prompt 管理页
├── (新增) src/api/system/prompt/index.ts      # API 调用
```

---

### 4.2 多Agent协作 + Context Store（原 4.1）

**方案**：拆分 Python executor 为多个专业化 Agent，由主控 Agent 统一调度。

**目录结构**：

```
python-executor/app/
├── main.py                    # FastAPI 入口
├── orchestrator.py            # 主控 Agent（调度 + 合并）
├── agents/
│   ├── __init__.py
│   ├── market_analyst.py      # 市场分析 Agent
│   ├── competitor_analyst.py  # 竞品分析 Agent
│   ├── profit_modeler.py      # 利润测算 Agent
│   ├── compliance_checker.py  # 合规检查 Agent
│   └── base_agent.py          # Agent 基类
├── context_store.py           # Context Store（共享状态）
├── guardrails/
│   ├── __init__.py
│   ├── l1_boundary.py
│   ├── l3_input.py
│   ├── l4_truth.py
│   ├── l5_provenance.py
│   └── l7_review.py
└── memory/
    ├── __init__.py
    ├── runtime_context.py
    ├── session_memory.py
    └── task_memory.py
```

**Context Store 设计**：

```python
# python-executor/app/context_store.py

class ContextStore:
    """Shared state across agents with write isolation."""
    def __init__(self):
        self._store: Dict[str, dict] = {}    # key → {value, version, owner, ttl}
        self._lock = threading.RLock()

    def read(self, key: str) -> Optional[dict]:
        with self._lock:
            entry = self._store.get(key)
            if entry and entry["ttl"] > time.time():
                return entry["value"]
            return None

    def write(self, key: str, value: dict, owner: str, ttl: int = 3600):
        """每个 Agent 只能写自己的 key，防止互相覆盖。"""
        with self._lock:
            existing = self._store.get(key)
            version = (existing["version"] + 1) if existing else 1
            self._store[key] = {
                "value": value, "version": version, "owner": owner,
                "written_at": time.time(), "ttl": time.time() + ttl
            }
```

**涉及文件**：新增 10+ 文件

---

### 4.2 自动评估引擎

**方案**：构建规则引擎 + 小模型辅助评分。

```python
# python-executor/app/evaluator.py

class AutoEvaluator:
    """Multi-dimension auto-evaluation for Agent outputs."""

    DIMENSIONS = {
        "accuracy":      "数据准确性 — 事实是否与来源一致",
        "completeness":  "完整性 — 是否覆盖用户问题的所有维度",
        "relevance":     "相关性 — 是否紧扣选品主题",
        "safety":        "安全性 — 无红线内容、无编造",
        "actionability": "可操作性 — 是否有明确的下一步建议",
    }

    def evaluate(self, report_md: str, provenance: dict,
                 expected_data_points: list) -> EvaluationResult:
        scores = {}
        scores["accuracy"] = self._score_accuracy(report_md, provenance)
        scores["completeness"] = self._score_completeness(report_md, expected_data_points)
        scores["safety"] = self._score_safety(report_md)
        scores["overall"] = sum(scores.values()) / len(scores)
        return EvaluationResult(scores=scores, ...)
```

---

## 5. V2.0+ 智能化（P4）

### 5.1 四层记忆体系

```python
# python-executor/app/memory/

class MemoryManager:
    """Tiered memory: Runtime → Session → Task → Long-term."""

    def __init__(self, redis_client, db_session):
        self.runtime = RuntimeMemory()         # 单次调用，内存 dict
        self.session = SessionMemory(redis)     # 单次会话，Redis hash，TTL=24h
        self.task = TaskMemory(db_session)      # 跨多轮，Postgres crob_task_event
        self.longterm = LongTermMemory(db)      # 永久，Postgres crob_longterm_memory

    # 写入规则：
    # - 可溯源的事实 → Task + LongTerm
    # - 不确定性内容 → Runtime only
    # - 红线相关 → 仅最小审计字段
```

### 5.2 生命周期 Hook 系统

```python
# python-executor/app/lifecycle_hooks.py

class LifecycleHookManager:
    """事件驱动 Hook 系统 — 关键动作确定性触发，不依赖 Prompt。"""

    HOOKS = {
        "on_scope_decided":    [log_decision, check_redline],
        "on_truth_failed":     [force_degrade, write_failure_reason],
        "on_quality_blocked":  [block_output, enqueue_human_review],
        "on_task_completed":   [dream_consolidation, archive_session],
        "on_user_feedback":    [update_eval_dataset, adjust_confidence],
    }

    def fire(self, event: str, context: dict):
        for handler in self.HOOKS.get(event, []):
            try:
                handler(context)
            except Exception as e:
                log.error(f"Hook {event}::{handler.__name__} failed: {e}")
```

---

## 6. 实施路线图

```
Week 1-2  │ P0: MVP 补全
          │ ├─ L3 输入校验强化
          │ ├─ L5 溯源包装全覆盖
          │ ├─ L7 基础质量审核
          │ └─ 前端 Chat 展示溯源标记
          │
Week 3-6  │ P1: V1.0 生产级
          │ ├─ L1 意图分类器（LLM-driven）
          │ ├─ L4 真实性校验升级（数据源分级）
          │ ├─ L2 业务监控面板
          │ ├─ 人工抽检流程 + 数据库表
          │ └─ Python executors 代码拆分（guardrails/ 模块）
          │
Week 7-12 │ P2: V1.5 规模化
          │ ├─ DAG 编排引擎
          │ ├─ Explore/Plan/Act 状态机
          │ ├─ L2 时效性校验
          │ └─ Fork-Join 并行执行
          │
Week 13-24│ P3: V2.0 质量达标
          │ ├─ Prompt 模板管理（工作空间文件系统 + 装配引擎）
          │ ├─ Prompt 管理 API + 前端编辑/预览页面
          │ ├─ 多Agent 拆分
          │ ├─ Context Store
          │ ├─ 自动评估引擎
          │ └─ 评估数据集构建（≥200条）
          │
Week 25+  │ P4: V2.0+ 智能化
          │ ├─ 四层记忆体系
          │ ├─ Dream Consolidation
          │ ├─ 生命周期 Hook 系统
          │ └─ 渐进式上下文压缩
```

---

## 7. 关键新增文件清单

```
P0 (python-executor/):
  (修改) app/executor.py — 增加 _l3_check, _l5_provenance, _l7_review

P1 (java-control/):
  (新增) .../controller/admin/dashboard/DashboardController.java
  (新增) .../service/dashboard/DashboardService.java
  (新增) .../controller/admin/review/ReviewController.java
  (新增) .../dal/dataobject/review/CrobReviewDO.java
  (新增) deploy/postgres/migration/V2__add_review_table.sql

P1 (python-executor/):
  (修改) app/executor.py — 增加 intent_classifier, _l4_truth_verify

P2 (python-executor/):
  (新增) app/dag_executor.py
  (新增) app/step_handlers/

P3 (python-executor/):
  (新增) workspaces/                    # Agent 工作空间（20+ 目录/文件）
  (新增) app/prompt_assembler.py        # Prompt 装配引擎
  (新增) app/prompt_loader.py          # 文件加载 + frontmatter 解析
  (新增) app/orchestrator.py
  (新增) app/agents/
  (新增) app/context_store.py
  (新增) app/evaluator.py

P3 (java-control/):
  (新增) .../controller/admin/prompt/PromptTemplateController.java
  (新增) .../service/prompt/PromptTemplateService.java

P3 (web-admin/):
  (新增) src/views/system/prompt/index.vue
  (新增) src/api/system/prompt/index.ts

P4 (python-executor/):
  (新增) app/memory/
  (新增) app/lifecycle_hooks.py
```

---

> **核心原则回顾**：边界宁紧勿松，误判宁拒勿错；接受 Agent "不知道"，绝不容忍"编造"。
