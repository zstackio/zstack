package org.zstack.core.scheduler;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.zstack.utils.gson.JSONObjectUtil;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by Mei Lei on 7/14/16.
 */
public class SchedulerRunner implements Job {
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
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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
