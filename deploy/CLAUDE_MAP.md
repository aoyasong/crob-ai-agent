# CLAUDE_MAP.md — deploy 目录功能模块说明

## 目录结构

```
deploy/
├── docker-compose.yml        # 容器编排（4 个服务 + 1 个初始化任务）
├── .env                      # 本地环境变量（私有，不提交）
├── .env.example              # 环境变量模板
├── postgres/init/            # Postgres 初始化脚本
│   ├── 01_init.sql           # 数据库用户与权限
│   ├── 02_extensions.sql     # 扩展创建
│   ├── 10_ruoyi-vue-pro.sql  # 芋道框架表结构
│   ├── 11_quartz.sql         # Quartz 调度器表结构
│   └── 20_crob_schema.sql    # crob 业务表结构（task/attempt/event/result 等）
├── nginx/
│   └── crob-agent.conf       # Nginx 反代配置片段（SSE 关闭缓冲、禁止 /internal 透出）
└── http/                     # API 测试用例（curl/HTTP 文件）
    ├── create_market_analysis.json
    ├── create_market_missing_dataset.json
    ├── create_profit_negative_need_confirm.json
    ├── create_profit_positive.json
    └── submit_profit_confirm.json
```

## docker-compose 服务清单

| 服务名 | 镜像 | 对外端口 | 说明 |
|--------|------|----------|------|
| `minio` | minio/minio | 9000 / 9001 | 对象存储（evidence / audit-pack 桶） |
| `minio_init` | minio/mc | — | 桶初始化一次性任务 |
| `crob-java-control` | 本地构建 | 48080 | Java 控制面 |
| `crob-python-executor` | 本地构建 | 48081 | Python 执行面（容器内 8000） |
| `crob-web-admin` | 本地构建 | 48090 | Vue3 前端 |

## 基础设施约定

- Postgres 和 Redis **不在 compose 内启动**，要求宿主机已有并加入 `infrastructure_env_network`
- Redis 统一使用 DB=1 且需要密码
- MinIO 桶：`evidence`（输出证据）、`audit-pack`（审计包）
- 所有服务间通信走 `infrastructure_env_network`
- 容器访问外部基础设施使用 service 名：`postgres`、`redis`

## 启动命令

```bash
docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build
```
