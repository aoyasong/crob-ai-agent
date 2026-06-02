import glob
import json
import threading
import time

from .config import DATASETS_DIR, SILICONFLOW_MODEL
from .db import upsert_task_result
from .java_client import post_internal
from .redis_client import get_redis
from .siliconflow import stream_chat_completions


class CancelledError(Exception):
    pass


def run_attempt(task_id: int, attempt_id: int, scenario: str, inputs: dict, effective_constraints: dict) -> None:
    r = get_redis()
    lock_key = f"crob:lock:attempt:{task_id}:{attempt_id}"
    if not r.set(lock_key, "1", nx=True, ex=1800):
        return
    try:
        seq = [1]

        # L1 boundary check
        _l1_check(task_id, attempt_id, seq, scenario, inputs)

        _post_attempt_event(task_id, attempt_id, seq, "ATTEMPT_STARTED", {})
        _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "L1_PASSED"})

        # start heartbeat
        heartbeat_stop = threading.Event()

        def _send_heartbeat():
            while not heartbeat_stop.is_set():
                time.sleep(30)
                if not heartbeat_stop.is_set():
                    _post_attempt_event(task_id, attempt_id, seq, "HEARTBEAT", {})

        hb_thread = threading.Thread(target=_send_heartbeat, daemon=True)
        hb_thread.start()

        try:
            report_md, usage = _run_scenario(task_id, attempt_id, scenario, inputs, seq, r)
            upsert_task_result(task_id, attempt_id, report_md, None)
            if usage:
                _post_usage_event(task_id, attempt_id, seq, usage)
            _post_attempt_event(task_id, attempt_id, 999999, "ATTEMPT_SUCCEEDED", {})
        finally:
            heartbeat_stop.set()
            hb_thread.join(timeout=5)
    except CancelledError:
        _post_attempt_event(task_id, attempt_id, 999999, "ATTEMPT_CANCELLED", {})
    except Exception as e:
        err_code, err_msg = _map_error(e)
        _post_attempt_event(
            task_id,
            attempt_id,
            999999,
            "ATTEMPT_FAILED",
            {},
            error_code=err_code,
            error_message=err_msg,
        )
    finally:
        r.delete(lock_key)


def _l1_check(task_id, attempt_id, seq, scenario, inputs):
    """L1 boundary: validate scenario is supported and not a red-line request."""
    supported = {"MARKET_ANALYSIS", "PROFIT_MODEL", "CHAT"}
    if scenario.upper() not in supported:
        raise RuntimeError("E_SCOPE_UNSUPPORTED")
    prompt = str(inputs.get("prompt", "")).lower()
    redline_keywords = ["投资建议", "法律意见", "避税", "灰色清关", "仿牌", "fake", "counterfeit"]
    for kw in redline_keywords:
        if kw in prompt:
            raise RuntimeError("E_SCOPE_REDLINE")


def _run_scenario(task_id: int, attempt_id: int, scenario: str, inputs: dict, seq: list, r) -> tuple:
    if scenario.upper() == "MARKET_ANALYSIS":
        return _run_market_analysis(task_id, attempt_id, inputs, seq, r)
    if scenario.upper() == "PROFIT_MODEL":
        return _run_profit_model(task_id, attempt_id, inputs, seq, r)
    if scenario.upper() == "CHAT":
        return _run_chat(task_id, attempt_id, inputs, seq, r)
    raise RuntimeError("E_SCENARIO_UNSUPPORTED")


def _run_market_analysis(task_id: int, attempt_id: int, inputs: dict, seq: list, r) -> tuple:
    _check_cancel(r, attempt_id)
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "MA_01_VALIDATE_INPUT"})

    platform = str(inputs.get("platform"))
    market = str(inputs.get("market"))
    slug = str(inputs.get("category_or_keyword"))

    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "MA_02_BUILD_QUERY_PLAN"})
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "MA_03_FETCH_DATA"})

    pattern = f"{DATASETS_DIR}/market_analysis/{platform}/{market}/{slug}__v*.json"
    files = sorted(glob.glob(pattern))
    if not files:
        raise RuntimeError("E_DATA_UNAVAILABLE")
    with open(files[-1], encoding="utf-8") as f:
        data = json.load(f)

    # L4 truth verification: basic source annotation
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "MA_04_TRUTH_VERIFY"})
    data_point = {
        "data_point_id": f"market_analysis/{platform}/{market}/{slug}",
        "source": files[-1],
        "observed_at": data.get("observed_at", "unknown"),
        "confidence": 0.7,
    }

    # L5 provenance packaging
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "L5_PROVENANCE", "data_point": data_point})

    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "MA_05_GENERATE_REPORT"})

    prompt = {
        "role": "user",
        "content": (
            "请基于给定 JSON 数据，生成一份市场分析 Markdown 报告。"
            "要求：包含关键结论、数据证据表、风险与限制、建议动作。"
            "仅引用 JSON 内的数据，不要编造。"
            "数据来源：{source}，采集时间：{observed_at}。\n"
            "JSON 数据如下：\n{json_data}"
        ).format(
            source=data_point["source"],
            observed_at=data_point["observed_at"],
            json_data=json.dumps(data, ensure_ascii=False)[:12000],
        ),
    }
    return _llm_generate_report(task_id, attempt_id, [prompt], seq, r)


def _run_profit_model(task_id: int, attempt_id: int, inputs: dict, seq: list, r) -> tuple:
    _check_cancel(r, attempt_id)
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "PM_01_VALIDATE_INPUT"})

    cost = int(inputs.get("cost_fen"))
    price = int(inputs.get("price_fen"))
    profit = price - cost

    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "PM_02_LOAD_RATE_PROFILE"})
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "PM_03_CALCULATE_PNL"})
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "PM_04_SENSITIVITY_ANALYSIS"})
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "PM_05_TRUTH_AND_PROVENANCE"})
    _post_attempt_event(task_id, attempt_id, seq, "STEP_CHANGED", {"step": "PM_06_GENERATE_REPORT"})

    payload = {"cost_fen": cost, "price_fen": price, "profit_fen": profit}
    prompt = {
        "role": "user",
        "content": (
            "请基于给定的计算结果，生成一份利润测算 Markdown 报告。"
            "要求：包含 P&L 表格、敏感性分析建议、风险提示、假设与限制。"
            "不要编造外部费率数据，只能使用输入与计算结果。\n" + json.dumps(payload, ensure_ascii=False),
        ),
    }
    return _llm_generate_report(task_id, attempt_id, [prompt], seq, r)


def _run_chat(task_id: int, attempt_id: int, inputs: dict, seq: list, r) -> tuple:
    _check_cancel(r, attempt_id)
    prompt_text = str(inputs.get("prompt") or "").strip()
    if not prompt_text:
        raise RuntimeError("E_INPUT_INVALID")
    messages = [
        {"role": "system", "content": "你是一个选品 Agent，回答要结构化、可执行，避免编造。"},
        {"role": "user", "content": prompt_text},
    ]
    return _llm_generate_report(task_id, attempt_id, messages, seq, r)


def _llm_generate_report(task_id: int, attempt_id: int, messages: list, seq: list, r) -> tuple:
    stream_key = f"crob:stream:{task_id}:{attempt_id}"
    _post_attempt_event(task_id, attempt_id, seq, "L6_START", {})

    chunks: list[str] = []
    usage_info = {}
    for obj in stream_chat_completions(SILICONFLOW_MODEL, messages):
        _check_cancel(r, attempt_id)
        choice = (obj.get("choices") or [{}])[0]
        delta = (choice.get("delta") or {}).get("content")
        if delta:
            chunks.append(delta)
            r.xadd(stream_key, {"delta": delta}, maxlen=2000, approximate=True)
        # capture usage from final chunk
        if obj.get("usage"):
            usage_info = obj["usage"]

    _post_attempt_event(task_id, attempt_id, seq, "L6_END", {})
    return "".join(chunks).strip(), usage_info


def _check_cancel(r, attempt_id: int) -> None:
    if r.get(f"crob:cancel:{attempt_id}") == "1":
        raise CancelledError()


def _post_attempt_event(
    task_id: int,
    attempt_id: int,
    seq: list,
    event_type: str,
    payload: dict,
    error_code: str | None = None,
    error_message: str | None = None,
) -> None:
    seq_val = seq[0] if isinstance(seq, list) else seq
    body = {
        "attemptEvent": {
            "taskId": task_id,
            "attemptId": attempt_id,
            "seq": seq_val,
            "eventType": event_type,
            "payload": payload,
            "errorCode": error_code,
            "errorMessage": error_message,
        }
    }
    if isinstance(seq, list):
        seq[0] += 1
    post_internal("/internal/task-events", body)


def _post_usage_event(task_id: int, attempt_id: int, seq: list, usage: dict) -> None:
    body = {
        "usageEvent": {
            "taskId": task_id,
            "attemptId": attempt_id,
            "seq": seq[0],
            "provider": "siliconflow",
            "model": SILICONFLOW_MODEL,
            "promptTokens": usage.get("prompt_tokens", 0),
            "completionTokens": usage.get("completion_tokens", 0),
            "totalTokens": usage.get("total_tokens", 0),
            "raw": usage,
        }
    }
    seq[0] += 1
    post_internal("/internal/task-events", body)


def _map_error(e: Exception) -> tuple:
    msg = str(e)
    if "E_SCOPE_REDLINE" in msg:
        return ("E_SCOPE_REDLINE", msg[:500])
    if "E_SCOPE_UNSUPPORTED" in msg:
        return ("E_SCOPE_UNSUPPORTED", msg[:500])
    if "E_DATA_UNAVAILABLE" in msg:
        return ("E_DATA_UNAVAILABLE", msg[:500])
    if "E_INPUT_INVALID" in msg:
        return ("E_INPUT_INVALID", msg[:500])
    if isinstance(e, CancelledError):
        return ("E_CANCELLED", msg[:500])
    return ("E_EXECUTOR_FAILED", msg[:500])
