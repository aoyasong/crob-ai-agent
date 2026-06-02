## web-admin/yudao-ui-admin-vue3（前端）协作指南

### 1) 目录定位
- 入口：`src/main.ts`
- 对话页：`src/views/crob/chat/index.vue`
- 路由注册：`src/router/modules/remaining.ts`（`/crob/chat`）
- 登录白名单：`src/permission.ts`（允许未登录直达 `/crob/chat`）

### 2) 本地开发
- 启动：`pnpm dev`（使用 `--mode env.local`）
- 默认端口：`48090`（见 `.env.local` 的 `VITE_PORT`）
- API 前缀：`/api`（不要用 `/admin-api`）

### 3) 与后端联调关键点
- 创建任务：`POST {VITE_BASE_URL}{VITE_API_URL}/tasks`
- 订阅流：`GET  {VITE_BASE_URL}{VITE_API_URL}/tasks/{taskId}/stream?attempt_id={attemptId}`

