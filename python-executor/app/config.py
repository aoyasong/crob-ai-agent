import os
from pathlib import Path

from dotenv import load_dotenv

# 从项目根目录加载 .env（向上查找，找到第一个 .env 文件）
_env_file = Path(__file__).resolve().parent.parent / ".env"
if _env_file.exists():
    load_dotenv(_env_file)
else:
    load_dotenv()


def getenv(name: str, default: str | None = None) -> str:
    v = os.getenv(name, default)
    if v is None:
        raise RuntimeError(f"missing env: {name}")
    return v


INTERNAL_HMAC_SECRET = getenv("INTERNAL_HMAC_SECRET")
JAVA_INTERNAL_BASE_URL = getenv("JAVA_INTERNAL_BASE_URL")
EXECUTOR_REDIS_URL = getenv("EXECUTOR_REDIS_URL")
EXECUTOR_DB_URL = getenv("EXECUTOR_DB_URL")
SILICONFLOW_API_KEY = getenv("SILICONFLOW_API_KEY")
SILICONFLOW_BASE_URL = os.getenv("SILICONFLOW_BASE_URL", "https://api.siliconflow.cn/v1")
SILICONFLOW_MODEL = os.getenv("SILICONFLOW_MODEL", "deepseek-ai/DeepSeek-V3")
DATASETS_DIR = os.getenv("DATASETS_DIR", "/datasets")
