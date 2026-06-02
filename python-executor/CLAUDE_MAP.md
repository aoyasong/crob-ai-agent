# CLAUDE_MAP.md — python-executor 目录功能模块说明

## 目录结构

```
python-executor/
├── app/
│   ├── main.py           # FastAPI 入口，路由注册、HMAC 校验、/health 和 /internal/execute
│   ├── config.py         # 环境变量读取与默认值定义
│   ├── executor.py       # 核心编排：run_attempt() 状态机，调用 LLM 并回传事件
│   ├── hmac_utils.py     # HMAC-SHA256 签名生成与校验工具函数
│   ├── java_client.py    # Java 内部接口 HTTP 客户端（POST /internal/task-events）
│   ├── redis_client.py   # Redis 连接工厂（用于 Stream 写入和 nonce 管理）
│   ├── siliconflow.py    # 硅基流动 OpenAI-compatible API 封装
│   └── db.py             # Postgres 数据库连接与操作
├── requirements.txt      # Python 依赖
├── Dockerfile            # 容器构建
└── CLAUDE.md             # 协作指南
```

## 核心流程

```
POST /internal/execute → HMAC 校验 → background_tasks: run_attempt()
                                            │
                                            ├── 场景步骤编排
                                            ├── 调用 LLM（siliconflow.py）
                                            ├── 写 Redis Stream (delta 事件)
                                            └── 回传 Java (POST /internal/task-events)
```

## 关键文件说明

| 文件 | 职责 | 修改频率 |
|------|------|----------|
| `main.py` | API 入口、HMAC 验证、请求分发 | 低（新增场景/接口时改） |
| `config.py` | 环境变量定义 | 低（新增配置项时改） |
| `executor.py` | 场景执行编排 | **高**（核心业务逻辑） |
| `java_client.py` | 向 Java 回传事件 | 低 |
| `siliconflow.py` | LLM API 调用 | 中（新增 provider 时改） |
| `db.py` | 数据库操作 | 中（新增表操作时改） |
