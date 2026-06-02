## python-executor（执行面）协作指南

### 1) 目录定位
- FastAPI 入口：`app/main.py`
- 配置读取：`app/config.py`
- 内部 HMAC：`app/hmac_utils.py`
- Redis：`app/redis_client.py`
- LLM 调用编排：`app/executor.py`
- Java 回调客户端：`app/java_client.py`
- 硅基流动 provider：`app/siliconflow.py`
- 数据库：`app/db.py`

### 2) Python 环境
- 本机使用 miniconda3，路径：`D:/Program Files/miniconda3`
- Python 版本：3.13，环境名：`p3.13`
- 所有命令执行前必须先激活环境：`source "D:/Program Files/miniconda3/etc/profile.d/conda.sh" && conda activate p3.13`

### 3) 运行方式
- 宿主机模式：先激活 conda 环境，然后 `uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload`
- 容器模式：由 `deploy/docker-compose.yml` 构建并启动（映射到宿主机 48081）
- 安装依赖：激活环境后 `pip install -r requirements.txt`

### 4) 格式化与 Lint
- 格式化：`ruff format .`
- Lint 检查：`ruff check .`
- Lint 自动修复：`ruff check --fix .`

### 5) 必要环境变量
- `EXECUTOR_DB_URL`（形如 `postgresql://user:pwd@host:5432/crob_agent`）
- `EXECUTOR_REDIS_URL`（形如 `redis://:pwd@host:6379/1`，DB 必须为 1）
- `JAVA_INTERNAL_BASE_URL`（容器内一般是 `http://crob-java-control:48080`）
- `INTERNAL_HMAC_SECRET`
- `SILICONFLOW_API_KEY`
- `SILICONFLOW_MODEL`（默认 `deepseek-ai/DeepSeek-V3`）
- `DATASETS_DIR`（默认 `/datasets`）

### 6) 与 Java 的契约要点
- Java → Python：`POST /internal/execute`（HMAC 必须通过）
- Python → Java：`POST /internal/task-events`（HMAC 必须通过）
- 流式输出：Python 写 Redis Stream（key 格式 `crob:stream:{task_id}:{attempt_id}`），Java 负责 SSE 转发给前端
