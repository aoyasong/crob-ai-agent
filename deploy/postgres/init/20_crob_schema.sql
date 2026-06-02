CREATE TABLE IF NOT EXISTS crob_task (
  task_id BIGINT PRIMARY KEY,
  scenario VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  inputs_json TEXT,
  current_attempt_id BIGINT,
  latest_success_attempt_id BIGINT,
  error_code VARCHAR(64),
  error_message VARCHAR(512),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  updated_by VARCHAR(64) NOT NULL
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
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  updated_by VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_crob_attempt_task_id ON crob_attempt (task_id);

CREATE TABLE IF NOT EXISTS crob_task_event (
  event_id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL,
  attempt_id BIGINT NOT NULL,
  seq INTEGER NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload_json TEXT,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  updated_by VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_crob_task_event_task_attempt_seq ON crob_task_event (task_id, attempt_id, seq);

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
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  updated_by VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_crob_usage_event_task_attempt_seq ON crob_usage_event (task_id, attempt_id, seq);

CREATE TABLE IF NOT EXISTS crob_task_result (
  task_id BIGINT NOT NULL,
  attempt_id BIGINT NOT NULL,
  report_md TEXT NOT NULL,
  result_json TEXT,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  updated_by VARCHAR(64) NOT NULL,
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
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  updated_by VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_crob_artifact_task_id ON crob_artifact (task_id);
