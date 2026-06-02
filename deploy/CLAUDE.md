## deploy（单机部署编排）协作指南

### 1) 目录定位
- 编排：`docker-compose.yml`
- 变量：`.env`（本地私有，不应提交；参考 `.env.example`）
- Postgres 初始化脚本：`postgres/init/`
- Nginx 配置片段：`nginx/`

### 2) 核心约定
- Postgres/Redis 不在本 compose 内启动，复用外部基础设施
- 本 compose 使用 external network：`infrastructure_env_network`
- Redis 统一使用 DB=1 且需要密码（变量 `CROB_REDIS_PW`）

### 3) 启动（容器模式）
- `docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build`

### 4) 服务清单
- `crob-java-control`（对外 48080）
- `crob-python-executor`（对外 48081）
- `crob-web-admin`（对外 48090）
- `minio` + `minio_init`（桶初始化：evidence/audit-pack）

