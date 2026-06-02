import json
from collections.abc import Iterator

import httpx

from .config import SILICONFLOW_API_KEY, SILICONFLOW_BASE_URL


def stream_chat_completions(model: str, messages: list[dict], timeout_s: int = 120) -> Iterator[dict]:
    url = SILICONFLOW_BASE_URL.rstrip("/") + "/chat/completions"
    headers = {"Authorization": f"Bearer {SILICONFLOW_API_KEY}"}
    payload = {"model": model, "messages": messages, "stream": True}
    with httpx.Client(timeout=timeout_s) as client:
        with client.stream("POST", url, headers=headers, json=payload) as resp:
            resp.raise_for_status()
            for line in resp.iter_lines():
                if not line:
                    continue
                if line.startswith("data:"):
                    data = line[5:].strip()
                else:
                    data = line.strip()
                if data == "[DONE]":
                    return
                try:
                    yield json.loads(data)
                except json.JSONDecodeError:
                    continue
