package org.zstack.sdk.disasterrecovery;

import org.zstack.sdk.disasterrecovery.ScheduleJobStatus;

public class ScheduleJobResult  {

    public java.util.List jobResults;
    public void setJobResults(java.util.List jobResults) {
        this.jobResults = jobResults;
    }
    public java.util.List getJobResults() {
        return this.jobResults;
    }

    public ScheduleJobStatus status;
    public void setStatus(ScheduleJobStatus status) {
        this.status = status;
    }
    public ScheduleJobStatus getStatus() {
        return this.status;
    }

}
