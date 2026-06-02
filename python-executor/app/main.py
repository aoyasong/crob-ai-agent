import time

from fastapi import BackgroundTasks, FastAPI, Header, HTTPException, Request
from pydantic import BaseModel

from .config import INTERNAL_HMAC_SECRET
from .executor import run_attempt
from .hmac_utils import sha256_hex, sign_hex
from .redis_client import get_redis

app = FastAPI()


class ExecuteReq(BaseModel):
    task_id: int
    attempt_id: int
    scenario: str
    inputs: dict = {}
    effective_constraints_json: dict = {}


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/internal/execute")
async def internal_execute(
    request: Request,
    background_tasks: BackgroundTasks,
    x_timestamp: str | None = Header(default=None, alias="X-Timestamp"),
    x_nonce: str | None = Header(default=None, alias="X-Nonce"),
    x_signature: str | None = Header(default=None, alias="X-Signature"),
):
    await _verify_hmac(request, x_timestamp, x_nonce, x_signature)
    try:
        payload = ExecuteReq.model_validate(await request.json())
    except Exception as e:
        raise HTTPException(status_code=400, detail="E_BAD_REQUEST") from e
    background_tasks.add_task(
        run_attempt,
        payload.task_id,
        payload.attempt_id,
        payload.scenario,
        payload.inputs or {},
        payload.effective_constraints_json or {},
    )
    return {"ok": True}


async def _verify_hmac(request: Request, timestamp: str | None, nonce: str | None, signature: str | None) -> None:
    if not timestamp or not nonce or not signature:
        raise HTTPException(status_code=401, detail="E_HMAC_MISSING")
    try:
        ts = int(timestamp)
    except ValueError as e:
        raise HTTPException(status_code=401, detail="E_HMAC_INVALID") from e
    now = int(time.time() * 1000)
    if abs(now - ts) > 60_000:
        raise HTTPException(status_code=401, detail="E_HMAC_EXPIRED")

    r = get_redis()
    if not r.set(f"crob:nonce:internal:{nonce}", "1", nx=True, ex=90):
        raise HTTPException(status_code=401, detail="E_HMAC_REPLAY")

    body = await request.body()
    canonical = timestamp + "\n" + request.method + "\n" + request.url.path + "\n" + sha256_hex(body)
    expected = sign_hex(INTERNAL_HMAC_SECRET, canonical)
    if expected.lower() != signature.lower():
        raise HTTPException(status_code=401, detail="E_HMAC_INVALID")
