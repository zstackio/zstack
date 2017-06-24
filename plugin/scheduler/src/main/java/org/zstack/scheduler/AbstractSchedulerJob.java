package org.zstack.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by Mei Lei<meilei007@gmail.com> on 8/3/16.
 */

public class AbstractSchedulerJob implements SchedulerJob {
    @Autowired
    protected transient CloudBus bus;

    private String name;
    private String description;
    private String jobName;
    private String resourceUuid;
    private String targetResourceUuid;
    private Timestamp createDate;
    private String accountUuid;

    public AbstractSchedulerJob() {
    }

    public AbstractSchedulerJob(APICreateSchedulerJobMsg msg) {
        String jobIdentifyUuid = Platform.getUuid();
        Date date = new Date();
        createDate = new Timestamp(date.getTime());
        name = msg.getName();
        resourceUuid = msg.getResourceUuid();
        accountUuid = msg.getSession().getAccountUuid();

        if ( msg.getDescription() != null && ! msg.getDescription().isEmpty()) {
            description = msg.getDescription();
        }

        // jobName, jobGroup, triggerName, triggerGroup reserved for future API
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {

        this.createDate = createDate;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    @Override
    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public void run() {}

}
