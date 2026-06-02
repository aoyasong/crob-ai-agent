# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 业务背景

**选品 Agent 任务平台** — 为跨境电商卖家提供 AI 选品决策助手。

核心能力：接收卖家选品需求 → 编排多步骤分析流程 → 调用大模型生成选品建议 → 输出结构化报告。

三大产品目标（递进关系）：
1. **边界控制** — 只回答选品相关问题，能拒绝无关问题
2. **真实性保障** — 禁止编造数据，能力不足时坦诚反馈
3. **质量达标** — 综合输出质量达到 90% 以上

## 仓库导航

本仓库包含 4 个工程、2 个辅助目录。每个工程有各自的 `CLAUDE.md`（约定与命令）和 `CLAUDE_MAP.md`（目录功能模块说明），进入对应目录后会自动加载。

### 你需要做什么？→ 去哪个目录

| 需求 | 目录 | 说明 |
|------|------|------|
| 任务 API、调度、门禁、配置管理、SSE 转发 | `java-control/` | Spring Boot 2.7 + 芋道脚手架 |
| LLM 调用编排、provider 插件、场景步骤执行 | `python-executor/` | FastAPI |
| 管理后台页面、对话页 `/crob/chat` | `web-admin/yudao-ui-admin-vue3/` | Vue 3 + element-plus |
| Docker 编排、Nginx 配置、数据库初始化 | `deploy/` | docker-compose |
| 方案文档、契约定义 | `docs/` | 辅助目录，只读参考 |
| 静态数据集 | `datasets/` | 辅助目录，只读参考 |

### 三个工程的关系

```
前端 (web-admin) ──HTTP/SSE──→ Java (java-control) ──HMAC──→ Python (python-executor)
                                      ↑                        │
                                      └──HMAC 事件回调──────────┘
                                      ↑                        │
                                      └──Redis Stream (LLM 流式输出)──┘
```

- **Java** 是对外唯一入口，拥有权威状态，前端不直接访问 Python
- **Python** 只被 Java 调用，执行 LLM 编排，结果通过 HMAC 回传 + Redis Stream 流式输出
- **SSE 链路**：Python 写 Redis Stream → Java 读取并推送给前端

## 运行形态

- **容器模式**（推荐联调）：`docker compose -f deploy/docker-compose.yml --env-file deploy/.env up -d --build`
- **本地模式**：三个工程在宿主机分别启动，直连 `127.0.0.1` 上的 Postgres/Redis

## 关键端口

| 服务 | 端口 |
|------|------|
| Java 控制面 | `48080` |
| Python 执行面 | `48081`（容器内 8000） |
| Web Admin | `48090` |
| MinIO | `9000`（console 9001） |

## 关键环境变量（不要将真实值写入仓库）

- `INTERNAL_HMAC_SECRET` — Java ↔ Python 内部接口签名密钥
- `SILICONFLOW_API_KEY` — 大模型供应商 Key
- `CROB_DB_URL` / `CROB_DB_USERNAME` / `CROB_DB_PASSWORD`
- `CROB_REDIS_HOST` / `CROB_REDIS_PORT` / `CROB_REDIS_DB` / `CROB_REDIS_PW`

## 基础设施约定

- Postgres / Redis 不由本仓库启动，容器模式下要求在同一 external network：`infrastructure_env_network`
- Redis 使用 `DB=1`，且开启密码
- 数据库使用 `crob_agent`
- 所有对外 API 前缀为 `/api`；内部接口前缀为 `/internal`（HMAC 校验）

## 跨项目契约

涉及接口字段、错误码、事件类型、Redis key 结构的变更，必须 Java / Python / 前端三端同步。
