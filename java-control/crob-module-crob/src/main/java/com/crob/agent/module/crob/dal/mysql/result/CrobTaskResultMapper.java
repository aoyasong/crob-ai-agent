package com.crob.agent.module.crob.dal.mysql.result;

import com.crob.agent.module.crob.dal.dataobject.result.CrobTaskResultDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CrobTaskResultMapper {

    @Select(
            "SELECT task_id AS taskId, attempt_id AS attemptId, report_md AS reportMd, "
                    + "CAST(result_json AS TEXT) AS resultJson, "
                    + "created_at AS createdAt, updated_at AS updatedAt, created_by AS createdBy, updated_by AS updatedBy "
                    + "FROM crob_task_result "
                    + "WHERE task_id = #{taskId} AND attempt_id = #{attemptId}")
    CrobTaskResultDO select(@Param("taskId") Long taskId, @Param("attemptId") Long attemptId);

    @Insert(
            "INSERT INTO crob_task_result (task_id, attempt_id, report_md, result_json, created_at, updated_at, created_by, updated_by) "
                    + "VALUES (#{taskId}, #{attemptId}, #{reportMd}, CAST(#{resultJson} AS JSONB), #{createdAt}, #{updatedAt}, #{createdBy}, #{updatedBy}) "
                    + "ON CONFLICT (task_id, attempt_id) "
                    + "DO UPDATE SET report_md = EXCLUDED.report_md, "
                    + "result_json = EXCLUDED.result_json, "
                    + "updated_at = EXCLUDED.updated_at, "
                    + "updated_by = EXCLUDED.updated_by")
    void upsert(CrobTaskResultDO result);
}
