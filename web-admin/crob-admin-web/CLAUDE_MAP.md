# CLAUDE_MAP.md — web-admin 目录功能模块说明

## 顶层结构

```
web-admin/yudao-ui-admin-vue3/
├── src/
│   ├── main.ts              # 应用入口
│   ├── App.vue              # 根组件
│   ├── permission.ts        # 路由守卫与登录白名单（/crob/chat 允许未登录）
│   ├── api/                 # 后端 API 调用封装（按业务模块分目录）
│   ├── views/               # 页面视图（按业务模块分目录）
│   │   └── crob/chat/       # ★ 选品 Agent 对话页（核心页面）
│   ├── components/          # 全局公共组件
│   ├── router/              # 路由配置
│   │   └── modules/         # 按模块拆分的路由文件
│   ├── store/               # Pinia 状态管理
│   ├── hooks/               # 组合式函数（composables）
│   ├── directives/          # 自定义指令（权限校验等）
│   ├── layout/              # 页面布局组件
│   ├── config/              # 应用配置（axios 拦截器等）
│   ├── plugins/             # 插件注册（elementPlus、echarts、i18n 等）
│   ├── utils/               # 工具函数
│   ├── styles/              # 全局样式
│   ├── types/               # TypeScript 类型定义
│   ├── locales/             # 国际化语言包
│   └── assets/              # 静态资源
├── build/                   # 构建配置（Vite）
├── public/                  # 公共静态资源
├── types/                   # 全局类型声明
└── .env.local               # 本地环境变量（VITE_PORT=48090）
```

## 核心目录说明

### `src/api/` — API 调用层

按业务模块分目录，每个目录下包含该模块的接口封装：

| 子目录 | 对应后端模块 |
|--------|-------------|
| `api/system/` | 用户/角色/菜单/租户等 |
| `api/infra/` | 文件/配置/代码生成等 |
| `api/bpm/` | 工作流 |
| `api/ai/` | AI 大模型 |
| `api/crm/` | CRM |
| `api/erp/` | ERP |
| `api/mes/` | MES |
| `api/iot/` | IoT |
| `api/pay/` | 支付 |
| `api/mall/` | 商城 |
| `api/member/` | 会员 |
| `api/mp/` | 微信公众号 |
| `api/login/` | 登录认证 |

> 选品 Agent 的相关 API 将在 `api/` 下新增（或就近放在调用方），目前尚未独立成目录。

### `src/views/` — 页面视图

与 `api/` 对应，按业务模块分目录：

| 子目录 | 内容 |
|--------|------|
| `views/crob/chat/` | **选品 Agent 对话页**（本平台核心页面） |
| `views/system/` | 系统管理页 |
| `views/infra/` | 基础设施管理页 |
| `views/Home/` | 首页 |
| `views/Login/` | 登录页 |
| `views/Profile/` | 个人中心 |
| 其他 `views/*/` | 对应各业务模块的 CRUD 管理页 |

### `src/components/` — 公共组件

60+ 个通用组件，常用：
- `Table/` — 表格组件
- `Form/` — 表单组件
- `Search/` — 搜索组件
- `Dialog/` — 对话框组件
- `UploadFile/` — 文件上传
- `Editor/` — 富文本编辑器
- `Echart/` — 图表组件
- `Cropper/` — 图片裁剪

### `src/router/modules/` — 路由

`remaining.ts` 注册了 `/crob/chat` 路由（选品对话页）。

### `src/permission.ts` — 权限控制

路由守卫逻辑，`/crob/chat` 在白名单中允许未登录访问。
