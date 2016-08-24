package org.zstack.header.core.scheduler;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by root on 7/18/16.
 */
@Inventory(mappingVOClass = SchedulerVO.class, collectionValueOfMethod="valueOf1")
@PythonClassInventory
public class SchedulerInventory implements Serializable {
    private String uuid;
    private String targetResourceUuid;
    private String schedulerName;
    private String schedulerType;
    private Integer schedulerInterval;
    private Integer repeatCount;
    private String cronScheduler;
    private Timestamp createDate;
    private Timestamp startTime;
    private Timestamp lastOpDate;
    /**
     * @desc jobClassName define the job
     */
    private String jobClassName;
    private String jobData;
    private String status;

    protected SchedulerInventory(SchedulerVO vo) {
        uuid = vo.getUuid();
        targetResourceUuid = vo.getTargetResourceUuid();
        schedulerName = vo.getSchedulerName();
        schedulerType = vo.getSchedulerType();
        schedulerInterval = vo.getSchedulerInterval();
        repeatCount = vo.getRepeatCount();
        cronScheduler = vo.getCronScheduler();
        createDate = vo.getCreateDate();
        startTime = vo.getStartDate();
        lastOpDate = vo.getLastOpDate();
        jobClassName = vo.getJobClassName();
        jobData = vo.getJobData();
        status = vo.getStatus();
    }
    public SchedulerInventory() {

    }
    public static SchedulerInventory valueOf(SchedulerVO vo) {
        return new SchedulerInventory(vo);
    }

    public static List<SchedulerInventory> valueOf1(Collection<SchedulerVO> vos) {
        List<SchedulerInventory> invs = new ArrayList<SchedulerInventory>(vos.size());
        for (SchedulerVO vo : vos) {
            invs.add(SchedulerInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public int getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(int schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getCronScheduler() {
        return cronScheduler;
    }

    public void setCronScheduler(String cronScheduler) {
        this.cronScheduler = cronScheduler;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public void setSchedulerInterval(Integer schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }
}
