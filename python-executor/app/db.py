from psycopg import connect

from .config import EXECUTOR_DB_URL


def upsert_task_result(task_id: int, attempt_id: int, report_md: str, result_json: str | None) -> None:
    with connect(EXECUTOR_DB_URL) as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                INSERT INTO crob_task_result (task_id, attempt_id, report_md, result_json, created_at, updated_at, created_by, updated_by)
                VALUES (%s, %s, %s, %s, NOW(), NOW(), %s, %s)
                ON CONFLICT (task_id, attempt_id)
                DO UPDATE SET report_md = EXCLUDED.report_md,
                              result_json = EXCLUDED.result_json,
                              updated_at = NOW(),
                              updated_by = EXCLUDED.updated_by
                """,
                (task_id, attempt_id, report_md, result_json, "EXECUTOR:python", "EXECUTOR:python"),
            )
