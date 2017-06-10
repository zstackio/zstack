package org.zstack.scheduler;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

/**
 * Created by Mei Lei on 7/14/16.
 */
public class SchedulerRunner implements Job {
    private static final CLogger logger = Utils.getLogger(SchedulerRunner.class);
    private  String jobData;
    private String jobClassName;
    private SchedulerJob runnerJob;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        jobData = dataMap.getString("jobData");
        jobClassName = dataMap.getString("jobClassName");
        try {
            runnerJob = (SchedulerJob) JSONObjectUtil.toObject(jobData, Class.forName(jobClassName));
            runnerJob.run();
        } catch (Exception e) {
            logger.warn(String.format("Class %s Not found error", jobClassName));
            logger.warn(String.format("Class %s Not found error", runnerJob.getClass().getName()));
            throw new RuntimeException(e);
        }

    }

        public String getJobClassName() {
            return jobClassName;
        }


        public void setJobClassName(String jobClassName) {
            this.jobClassName = jobClassName;
        }


        public String getJobData() {
            return jobData;
        }

        public void setJobData(String jobData) {
            this.jobData = jobData;
        }

}
