from redis import Redis

from .config import EXECUTOR_REDIS_URL


def get_redis() -> Redis:
    return Redis.from_url(EXECUTOR_REDIS_URL, decode_responses=True)
