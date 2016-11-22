package io.github.bitornot.lts.ext.biz.log.oracle;

import io.github.bitornot.lts.ext.queue.oracle.support.RshHolder;
import io.github.bitornot.lts.ext.store.jdbc.builder.InsertSql;
import io.github.bitornot.lts.ext.store.jdbc.builder.SelectSql;
import io.github.bitornot.lts.ext.store.jdbc.oracle.OracleJdbcAbstractAccess;
import io.github.bitornot.lts.ext.utils.SqlUtils;

import java.math.BigDecimal;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.github.ltsopensource.admin.response.PaginationRsp;
import com.github.ltsopensource.biz.logger.JobLogger;
import com.github.ltsopensource.biz.logger.domain.JobLogPo;
import com.github.ltsopensource.biz.logger.domain.JobLoggerRequest;
import com.github.ltsopensource.core.cluster.Config;
import com.github.ltsopensource.core.commons.utils.CollectionUtils;
import com.github.ltsopensource.store.jdbc.builder.OrderByType;
import com.github.ltsopensource.store.jdbc.builder.WhereSql;
import com.github.ltsopensource.store.jdbc.dbutils.JdbcTypeUtils;

/**
 * Oracle任务日志扩展
 * 
 * created by fanlu on 11/14/2016
 */
public class OracleJobLogger extends OracleJdbcAbstractAccess implements JobLogger {
	
	private static final String DEFAULT_TABLE_NAME = "lts_job_log_po";

	public OracleJobLogger(Config config) {
		super(config);
		createTable(readSqlFile("sql/oracle/lts_job_log_po.sql", getTableName()), getTableName());
	}

	@Override
	public void log(JobLogPo jobLogPo) {
		
		if (jobLogPo == null) {
			return;
		}
		
		InsertSql insertSql = buildInsertSql();
		setInsertSqlValues(insertSql, jobLogPo).doInsert();
	}

	@Override
	public void log(List<JobLogPo> jobLogPos) {
		
		if (CollectionUtils.isEmpty(jobLogPos)) {
			return;
		}
		
		InsertSql insertSql = buildInsertSql();

        for (JobLogPo jobLogPo : jobLogPos) {
            setInsertSqlValues(insertSql, jobLogPo);
        }
        insertSql.doBatchInsert();
	}
	
    private InsertSql buildInsertSql() {
        return new InsertSql(getSqlTemplate())
                .insert(getTableName())
                .columns("id",
                		"log_time",
                        "gmt_created",
                        "log_type",
                        "success",
                        "msg",
                        "task_tracker_identity",
                        "log_level",
                        "task_id",
                        "real_task_id",
                        "job_id",
                        "job_type",
                        "priority",
                        "submit_node_group",
                        "task_tracker_node_group",
                        "ext_params",
                        "internal_ext_params",
                        "need_feedback",
                        "cron_expression",
                        "trigger_time",
                        "retry_times",
                        "max_retry_times",
                        "rely_on_prev_cycle",
                        "repeat_count",
                        "repeated_count",
                        "repeat_interval"
                );
    }

    private InsertSql setInsertSqlValues(InsertSql insertSql, JobLogPo jobLogPo) {
        return insertSql.values(SqlUtils.getIdFromTimestamp(),
        		jobLogPo.getLogTime(),
                jobLogPo.getGmtCreated(),
                jobLogPo.getLogType().name(),
                jobLogPo.isSuccess(),
                jobLogPo.getMsg(),
                jobLogPo.getTaskTrackerIdentity(),
                jobLogPo.getLevel().name(),
                jobLogPo.getTaskId(),
                jobLogPo.getRealTaskId(),
                jobLogPo.getJobId(),
                jobLogPo.getJobType() == null ? null : jobLogPo.getJobType().name(),
                jobLogPo.getPriority(),
                jobLogPo.getSubmitNodeGroup(),
                jobLogPo.getTaskTrackerNodeGroup(),
                JSON.toJSONString(jobLogPo.getExtParams()),
                JSON.toJSONString(jobLogPo.getInternalExtParams()),
                jobLogPo.isNeedFeedback(),
                jobLogPo.getCronExpression(),
                jobLogPo.getTriggerTime(),
                jobLogPo.getRetryTimes(),
                jobLogPo.getMaxRetryTimes(),
                jobLogPo.getDepPreCycle(),
                jobLogPo.getRepeatCount(),
                jobLogPo.getRepeatedCount(),
                jobLogPo.getRepeatInterval());
    }


	@Override
	public PaginationRsp<JobLogPo> search(JobLoggerRequest request) {

        PaginationRsp<JobLogPo> response = new PaginationRsp<JobLogPo>();

        BigDecimal results = new SelectSql(getSqlTemplate())
                .select()
                .columns("count(1)")
                .from()
                .table(getTableName())
                .whereSql(buildWhereSql(request))
                .single();
        response.setResults(results.intValue());
        if (results.intValue() == 0) {
            return response;
        }
        // 查询 rows
        List<JobLogPo> rows = new SelectSql(getSqlTemplate())
                .select()
                .all()
                .from()
                .table(getTableName())
                .whereSql(buildWhereSql(request))
                .orderBy()
                .column("log_time", OrderByType.DESC)
                .limit(request.getStart(), request.getLimit())
                .list(RshHolder.JOB_LOGGER_LIST_RSH);
        response.setRows(rows);

        return response;
	}

    private WhereSql buildWhereSql(JobLoggerRequest request) {
        return new WhereSql()
                .andOnNotEmpty("task_id = ?", request.getTaskId())
                .andOnNotEmpty("real_task_id = ?", request.getRealTaskId())
                .andOnNotEmpty("task_tracker_node_group = ?", request.getTaskTrackerNodeGroup())
                .andBetween("log_time", JdbcTypeUtils.toTimestamp(request.getStartLogTime()), JdbcTypeUtils.toTimestamp(request.getEndLogTime()))
                ;
    }
	
	private String getTableName() {
		return DEFAULT_TABLE_NAME;
	}

}