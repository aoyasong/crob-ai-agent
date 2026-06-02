## java-control（Crob 控制面）协作指南

### 1) 目录定位
- 应用入口：`Crob-server/`
- crob 业务模块：`Crob-module-crob/`
- 框架层：`Crob-framework/`（芋道官方框架，一般不改）

### 2) crob 模块架构

```
Crob-module-crob/
├── controller/admin/task/   → /api 对外接口（任务 CRUD、SSE、报告）
│   └── vo/                  → 请求/响应 DTO
├── controller/internal/     → /internal 内部接口（接收 Python 事件回调）
│   └── vo/                  → 内部接口 DTO
├── controller/report/       → 报告渲染接口
├── service/task/            → 业务逻辑（门禁、状态机、attempt 管理）
│   └── executor/            → Python 执行面 HTTP 客户端
├── dal/dataobject/          → 5 个核心实体：
│   ├── task/    CrobTaskDO
│   ├── attempt/ CrobAttemptDO
│   ├── event/   CrobTaskEventDO / CrobUsageEventDO
│   └── result/  CrobTaskResultDO
├── dal/mysql/               → MyBatis Plus Mapper
└── framework/security/      → InternalHmacFilter（拦截 /internal 校验签名与 nonce）
```

### 3) 流式架构（Redis Stream → SSE）

1. Python 调用 LLM 生成 delta 时写入 Redis Stream `crob:stream:{task_id}:{attempt_id}`
2. Java SSE 端点 `GET /api/tasks/{taskId}/stream` 从 Redis Stream 读取并推送给前端
3. 前端 EventSource 消费 SSE，断线后回退到轮询任务状态

### 4) 本地启动（宿主机模式）
- profile：`local`
- 默认端口：`48080`
- 常用环境变量
  - `CROB_DB_URL`（默认 `jdbc:postgresql://127.0.0.1:5432/crob_agent`）
  - `CROB_DB_USERNAME` / `CROB_DB_PASSWORD`
  - `CROB_REDIS_HOST=127.0.0.1` / `CROB_REDIS_PORT=6379` / `CROB_REDIS_DB=1` / `CROB_REDIS_PW`
  - `PYTHON_EXECUTOR_BASE_URL=http://127.0.0.1:48081`
  - `INTERNAL_HMAC_SECRET`

### 5) 容器模式
- Java 容器内通过 service 名访问：
  - Postgres：`postgres:5432`
  - Redis：`redis:6379`（DB=1）
  - Python executor：`http://crob-python-executor:8000`

### 6) 对外 API 与内部 API
- 对外前缀固定为：`/api`
  - 任务：`POST /api/tasks`、`GET /api/tasks/{taskId}`、`GET /api/tasks/{taskId}/stream?attempt_id=...`
- 内部前缀固定为：`/internal`
  - `POST /internal/execute`、`POST /internal/task-events`
  - 内部接口必须校验 HMAC（`INTERNAL_HMAC_SECRET`）

### 7) 常用命令（在 java-control 目录执行）
- 打包：`mvn -pl Crob-server -am -DskipTests package`
- 启动：`mvn -pl Crob-server -am spring-boot:run -Dspring-boot.run.profiles=local`
- 单测：`mvn -pl Crob-server -am test`
- 格式化：`mvn spotless:apply -pl Crob-module-crob`
- 格式检查：`mvn spotless:check -pl Crob-module-crob`
