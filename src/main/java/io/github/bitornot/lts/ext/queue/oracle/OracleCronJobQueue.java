package io.github.bitornot.lts.ext.queue.oracle;

import io.github.bitornot.lts.ext.queue.oracle.support.RshHolder;
import io.github.bitornot.lts.ext.store.jdbc.builder.DeleteSql;
import io.github.bitornot.lts.ext.store.jdbc.builder.SelectSql;

import com.github.ltsopensource.admin.request.JobQueueReq;
import com.github.ltsopensource.core.cluster.Config;
import com.github.ltsopensource.core.support.JobQueueUtils;
import com.github.ltsopensource.queue.CronJobQueue;
import com.github.ltsopensource.queue.domain.JobPo;

/**
 * created by fanlu on 11/14/2016
 */
public class OracleCronJobQueue extends OracleSchedulerJobQueue implements CronJobQueue {

    public OracleCronJobQueue(Config config) {
        super(config);
        createTable(readSqlFile("sql/oracle/lts_cron_job_queue.sql", getTableName()), getTableName());
    }

    @Override
    protected String getTableName(JobQueueReq request) {
        return getTableName();
    }

    @Override
    public boolean add(JobPo jobPo) {
        return super.add(getTableName(), jobPo);
    }

    @Override
    public JobPo getJob(String jobId) {
        return new SelectSql(getSqlTemplate())
                .select()
                .all()
                .from()
                .table(getTableName())
                .where("job_id = ?", jobId)
                .single(RshHolder.JOB_PO_RSH);
    }

    @Override
    public boolean remove(String jobId) {
        return new DeleteSql(getSqlTemplate())
                .delete()
                .from()
                .table(getTableName())
                .where("job_id = ?", jobId)
                .doDelete() == 1;
    }

    @Override
    public JobPo getJob(String taskTrackerNodeGroup, String taskId) {

        return new SelectSql(getSqlTemplate())
                .select()
                .all()
                .from()
                .table(getTableName())
                .where("task_id = ?", taskId)
                .and("task_tracker_node_group = ?", taskTrackerNodeGroup)
                .single(RshHolder.JOB_PO_RSH);
    }

    protected String getTableName() {
        return JobQueueUtils.CRON_JOB_QUEUE;
    }

}
