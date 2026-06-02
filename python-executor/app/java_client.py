import json
import time
import uuid

import httpx

from .config import INTERNAL_HMAC_SECRET, JAVA_INTERNAL_BASE_URL
from .hmac_utils import sha256_hex, sign_hex


def post_internal(path: str, body_json: dict) -> None:
    body_bytes = json.dumps(body_json, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    ts = str(int(time.time() * 1000))
    nonce = uuid.uuid4().hex
    canonical = ts + "\n" + "POST" + "\n" + path + "\n" + sha256_hex(body_bytes)
    signature = sign_hex(INTERNAL_HMAC_SECRET, canonical)
    headers = {
        "Content-Type": "application/json",
        "X-Timestamp": ts,
        "X-Nonce": nonce,
        "X-Signature": signature,
    }
    with httpx.Client(timeout=10.0) as client:
        client.post(JAVA_INTERNAL_BASE_URL + path, content=body_bytes, headers=headers)
