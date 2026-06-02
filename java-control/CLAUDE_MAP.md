# CLAUDE_MAP.md — java-control 目录功能模块说明

## 顶层结构

| 目录 | 说明 |
|------|------|
| `Crob-server/` | 应用启动入口，包含 Spring Boot 主类、全局配置、Dockerfile |
| `Crob-module-crob/` | **核心业务模块** — 选品 Agent 任务平台的全部功能 |
| `Crob-framework/` | 芋道框架层，提供通用基础设施能力（不改） |
| `Crob-dependencies/` | Maven 依赖版本管理 BOM（不改） |
| `Crob-module-system/` | 芋道系统功能模块：RBAC 用户/角色/菜单/租户/字典 |
| `Crob-module-infra/` | 芋道基础设施模块：代码生成、文件服务、定时任务、配置管理 |
| `Crob-module-bpm/` | 芋道工作流模块：Flowable 审批流程 |
| `Crob-module-pay/` | 芋道支付系统模块 |
| `Crob-module-member/` | 芋道会员中心模块 |
| `Crob-module-mall/` | 芋道商城系统模块（product/promotion/trade） |
| `Crob-module-crm/` | 芋道 CRM 客户关系管理模块 |
| `Crob-module-erp/` | 芋道 ERP 企业资源计划模块 |
| `Crob-module-mes/` | 芋道 MES 执行制造系统模块 |
| `Crob-module-wms/` | 芋道 WMS 仓库管理系统模块 |
| `Crob-module-ai/` | 芋道 AI 大模型模块 |
| `Crob-module-iot/` | 芋道 IoT 物联网模块 |
| `Crob-module-mp/` | 芋道微信公众号模块 |
| `Crob-module-report/` | 芋道大屏报表模块 |
| `sql/` | 数据库初始化 SQL（按数据库类型分目录） |
| `script/` | 部署/CI 脚本（docker、jenkins、shell） |
| `Crob-ui/` | 芋道前端工程（本平台使用 web-admin/，不依赖此目录） |

> **说明**：除 `Crob-module-crob` 外，其余 `Crob-module-*` 均为芋道官方模块，本平台仅复用其基础设施能力（RBAC、菜单、文件服务等），一般不做修改。

## Crob-module-crob 详细结构

```
Crob-module-crob/src/main/java/cn/iocoder/Crob/module/crob/
├── controller/
│   ├── admin/task/          # 对外 /api 接口
│   │   ├── CrobTaskController.java   # POST /api/tasks, GET /api/tasks/{id}, cancel, rerun, stream
│   │   └── vo/              # CrobTaskCreateReqVO, CrobTaskRespVO, CrobAttemptRespVO 等
│   ├── internal/            # 内部 /internal 接口（Java ↔ Python HMAC）
│   │   ├── CrobInternalTaskEventsController.java  # POST /internal/task-events
│   │   └── vo/              # InternalAttemptEventReqVO, InternalUsageEventReqVO
│   └── report/              # 报告渲染
│       └── CrobReportController.java
├── service/task/
│   ├── CrobTaskService.java        # 业务接口
│   ├── CrobTaskServiceImpl.java    # 业务实现（门禁、状态机、attempt 生命周期）
│   └── executor/
│       ├── CrobExecutorClient.java       # Python 调用接口
│       └── CrobExecutorClientImpl.java   # HTTP 客户端实现
├── dal/
│   ├── dataobject/          # 数据库实体（MyBatis Plus）
│   │   ├── task/CrobTaskDO.java
│   │   ├── attempt/CrobAttemptDO.java
│   │   ├── event/CrobTaskEventDO.java
│   │   ├── event/CrobUsageEventDO.java
│   │   └── result/CrobTaskResultDO.java
│   └── mysql/               # MyBatis Plus Mapper
│       ├── task/CrobTaskMapper.java
│       ├── attempt/CrobAttemptMapper.java
│       ├── event/CrobTaskEventMapper.java
│       ├── event/CrobUsageEventMapper.java
│       └── result/CrobTaskResultMapper.java
└── framework/security/       # HMAC 过滤器
    ├── config/SecurityConfiguration.java
    └── filter/
        ├── InternalHmacFilter.java              # HMAC 签名校验 + nonce 防重放
        ├── InternalHmacFilterConfiguration.java  # 过滤器注册
        └── CachedBodyHttpServletRequest.java     # 请求体缓存（支持重复读取）
```

## Crob-server 结构

```
Crob-server/src/main/
├── java/cn/iocoder/Crob/
│   └── CrobServerApplication.java  # Spring Boot 主类
└── resources/
    ├── application.yaml              # 全局配置
    ├── application-local.yaml        # local profile 配置（数据源、Redis、日志）
    └── ...
```

## Crob-framework 子模块

| 模块 | 功能 |
|------|------|
| `Crob-common` | 通用工具类、枚举、异常、结果封装 |
| `Crob-spring-boot-starter-web` | Web 层：全局异常处理、Swagger、访问日志 |
| `Crob-spring-boot-starter-security` | 安全框架：Spring Security + Token 鉴权 |
| `Crob-spring-boot-starter-mybatis` | 数据层：MyBatis Plus、多数据源、分页 |
| `Crob-spring-boot-starter-redis` | 缓存：Redisson、Redis Template、分布式锁 |
| `Crob-spring-boot-starter-mq` | 消息队列：Redis Stream / RabbitMQ / Kafka / RocketMQ |
| `Crob-spring-boot-starter-job` | 定时任务：Quartz 集成 |
| `Crob-spring-boot-starter-monitor` | 监控：Spring Boot Admin、Actuator |
| `Crob-spring-boot-starter-protection` | 服务保障：限流、幂等、分布式锁 |
| `Crob-spring-boot-starter-excel` | Excel 导入导出 |
| `Crob-spring-boot-starter-test` | 单元测试支持 |
| `Crob-spring-boot-starter-biz-tenant` | 多租户 |
| `Crob-spring-boot-starter-biz-data-permission` | 数据权限 |
| `Crob-spring-boot-starter-biz-ip` | IP 地址解析 |
| `Crob-spring-boot-starter-websocket` | WebSocket 支持 |
