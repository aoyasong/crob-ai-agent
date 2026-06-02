-- ============================================================
-- Crob Agent 数据库初始化脚本
-- 包含：系统表 + crob 业务表 + 种子数据
-- 使用：psql -h localhost -U postgres -d crob_agent -f init_crob.sql
-- ============================================================

BEGIN;

-- ============================================================
-- 1. 扩展
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 2. 系统表 - 租户
-- ============================================================
CREATE TABLE IF NOT EXISTS system_tenant (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    contact_name VARCHAR(64),
    contact_mobile VARCHAR(16),
    status SMALLINT DEFAULT 0,
    domain VARCHAR(128),
    package_id VARCHAR(64),
    expire_time TIMESTAMP,
    account_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 3. 系统表 - 部门
-- ============================================================
CREATE TABLE IF NOT EXISTS system_dept (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort INT DEFAULT 0,
    leader_user_id BIGINT,
    phone VARCHAR(16),
    email VARCHAR(128),
    status SMALLINT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 4. 系统表 - 用户
-- ============================================================
CREATE TABLE IF NOT EXISTS system_users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    nickname VARCHAR(64),
    dept_id BIGINT,
    email VARCHAR(128),
    mobile VARCHAR(16),
    sex SMALLINT DEFAULT 0,
    avatar VARCHAR(256),
    status SMALLINT DEFAULT 0,
    login_ip VARCHAR(64),
    login_date TIMESTAMP,
    remark VARCHAR(256),
    tenant_id BIGINT DEFAULT 1,
    creator VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- ============================================================
-- 5. 系统表 - 角色
-- ============================================================
CREATE TABLE IF NOT EXISTS system_role (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    sort INT DEFAULT 0,
    status SMALLINT DEFAULT 0,
    remark VARCHAR(256),
    tenant_id BIGINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 6. 系统表 - 菜单
-- ============================================================
CREATE TABLE IF NOT EXISTS system_menu (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    permission VARCHAR(128),
    type SMALLINT DEFAULT 1,
    sort INT DEFAULT 0,
    parent_id BIGINT DEFAULT 0,
    path VARCHAR(256),
    icon VARCHAR(64),
    component VARCHAR(256),
    component_name VARCHAR(64),
    status SMALLINT DEFAULT 0,
    visible BOOLEAN DEFAULT TRUE,
    keep_alive BOOLEAN DEFAULT TRUE,
    always_show BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 7. 系统表 - 用户角色关联
-- ============================================================
CREATE TABLE IF NOT EXISTS system_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1
);

-- ============================================================
-- 8. 系统表 - 角色菜单关联
-- ============================================================
CREATE TABLE IF NOT EXISTS system_role_menu (
    id BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    creator VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0,
    tenant_id BIGINT DEFAULT 1
);

-- ============================================================
-- 9. Crob 业务表
-- ============================================================
CREATE TABLE IF NOT EXISTS crob_task (
    task_id BIGINT PRIMARY KEY,
    scenario VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    inputs_json TEXT,
    current_attempt_id BIGINT,
    latest_success_attempt_id BIGINT,
    error_code VARCHAR(64),
    error_message VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT '',
    updated_by VARCHAR(64) NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS crob_attempt (
    attempt_id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    scenario VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    price_version_snapshot VARCHAR(64),
    effective_constraints_json TEXT,
    policy_version VARCHAR(64),
    last_heartbeat_at TIMESTAMP,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    error_code VARCHAR(64),
    error_message VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT '',
    updated_by VARCHAR(64) NOT NULL DEFAULT ''
);
CREATE INDEX IF NOT EXISTS idx_crob_attempt_task_id ON crob_attempt (task_id);

CREATE TABLE IF NOT EXISTS crob_task_event (
    event_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    seq INTEGER NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT '',
    updated_by VARCHAR(64) NOT NULL DEFAULT ''
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crob_task_event_seq ON crob_task_event (task_id, attempt_id, seq);

CREATE TABLE IF NOT EXISTS crob_usage_event (
    usage_id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    seq INTEGER NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(128) NOT NULL,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    cost_fen INTEGER,
    raw_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT '',
    updated_by VARCHAR(64) NOT NULL DEFAULT ''
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_crob_usage_event_seq ON crob_usage_event (task_id, attempt_id, seq);

CREATE TABLE IF NOT EXISTS crob_task_result (
    task_id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    report_md TEXT NOT NULL DEFAULT '',
    result_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT '',
    updated_by VARCHAR(64) NOT NULL DEFAULT '',
    PRIMARY KEY (task_id, attempt_id)
);

CREATE TABLE IF NOT EXISTS crob_artifact (
    artifact_id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    bucket VARCHAR(64) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    filename VARCHAR(255),
    content_type VARCHAR(128),
    size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(64) NOT NULL DEFAULT '',
    updated_by VARCHAR(64) NOT NULL DEFAULT ''
);
CREATE INDEX IF NOT EXISTS idx_crob_artifact_task_id ON crob_artifact (task_id);

-- ============================================================
-- 10. 种子数据 - 租户
-- ============================================================
INSERT INTO system_tenant (id, name, contact_name, status, domain, package_id, account_count)
VALUES (1, 'Crob', 'Admin', 0, 'localhost', 'default', 9999)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 11. 种子数据 - 角色
-- ============================================================
INSERT INTO system_role (id, name, code, sort, status, remark, tenant_id)
VALUES
    (1, '超级管理员', 'super_admin', 0, 0, '拥有所有权限', 1),
    (2, '普通用户',   'common',      1, 0, '基础权限',   1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 12. 种子数据 - 管理员用户 (密码: admin123)
-- ============================================================
-- 密码: admin123 (BCrypt $2a$04$)
INSERT INTO system_users (id, username, password, nickname, email, status, remark, tenant_id)
VALUES (1, 'admin', '$2a$04$sEtimsHu9YCkYY4/oqElHem2Ijc9ld20eYO6lN.g/21NfLUTDLB9W', 'Admin', 'admin@crob.io', 0, '系统管理员', 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 13. 种子数据 - 用户角色关联 (admin → super_admin)
-- ============================================================
INSERT INTO system_user_role (id, user_id, role_id, tenant_id)
VALUES (1, 1, 1, 1)
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- 14. 种子数据 - 菜单树
-- ============================================================
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show) VALUES
-- 一级菜单
(1,  '系统管理', '',         1, 0, 0, '/system',  'ep:setting',  '',           'System',        0, TRUE, TRUE, TRUE),
(2,  'Crob Agent', '',       1, 1, 0, '/crob',    'ep:chat-dot-round', '',     'Crob',          0, TRUE, TRUE, TRUE),
-- 二级菜单 - 系统管理
(10, '用户管理', 'system:user:list',   2, 0, 1, 'user',   'ep:user',   'system/user/index',    'SystemUser',    0, TRUE, TRUE, TRUE),
(11, '角色管理', 'system:role:list',   2, 1, 1, 'role',   'ep:user-filled', 'system/role/index', 'SystemRole', 0, TRUE, TRUE, TRUE),
(12, '菜单管理', 'system:menu:list',   2, 2, 1, 'menu',   'ep:menu',   'system/menu/index',    'SystemMenu',    0, TRUE, TRUE, TRUE),
	(13, '部门管理', 'system:dept:list',   2, 3, 1, 'dept',   'ep:office-building', 'system/dept/index', 'SystemDept', 0, TRUE, TRUE, TRUE),
-- 二级菜单 - Crob
(20, 'Agent对话', '',                 2, 0, 2, 'chat',   'ep:chat-line-square', 'crob/chat/index', 'CrobChat', 0, TRUE, TRUE, TRUE),
(21, '任务列表', 'crob:task:list',     2, 1, 2, 'task',   'ep:list',   'crob/task/index',      'CrobTask',      0, TRUE, TRUE, TRUE)
ON CONFLICT (id) DO UPDATE SET status = 0, name = EXCLUDED.name, path = EXCLUDED.path, component = EXCLUDED.component, sort = EXCLUDED.sort, parent_id = EXCLUDED.parent_id;

-- 确保所有菜单状态为启用（防止未知原因导致的 status=1）
UPDATE system_menu SET status = 0 WHERE status != 0;

-- ============================================================
-- 15. 种子数据 - 角色菜单关联（super_admin 拥有所有菜单）
-- ============================================================
INSERT INTO system_role_menu (id, role_id, menu_id, tenant_id)
VALUES
    (1,  1, 1,  1),
    (2,  1, 10, 1),
    (3,  1, 11, 1),
    (4,  1, 12, 1),
    (5,  1, 2,  1),
    (6,  1, 20, 1),
	    (7,  1, 21, 1),
	    (8,  1, 13, 1)
ON CONFLICT (id) DO NOTHING;

COMMIT;
