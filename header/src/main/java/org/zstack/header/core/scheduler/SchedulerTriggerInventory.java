package org.zstack.header.core.scheduler;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by AlanJager on 2017/6/7.
 */

@Inventory(mappingVOClass = SchedulerTriggerVO.class)
@PythonClassInventory
public class SchedulerTriggerInventory {
    private String uuid;
    private String name;
    private String description;
    private String schedulerType;
    private Integer schedulerInterval;
    private Integer repeatCount;
    private Timestamp startTime;
    private Timestamp stopTime;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected SchedulerTriggerInventory(SchedulerTriggerVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.schedulerType = vo.getSchedulerType();
        this.schedulerInterval = vo.getSchedulerInterval();
        this.repeatCount = vo.getRepeatCount();
        this.startTime = vo.getStartTime();
        this.stopTime = vo.getStopTime();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public SchedulerTriggerInventory() {

    }

    public static SchedulerTriggerInventory valueOf(SchedulerTriggerVO vo) {
        return new SchedulerTriggerInventory(vo);
    }

    public static List<SchedulerTriggerInventory> valueOf(Collection<SchedulerTriggerVO> vos) {
        List<SchedulerTriggerInventory> invs = new ArrayList<SchedulerTriggerInventory>(vos.size());
        for (SchedulerTriggerVO vo : vos) {
            invs.add(SchedulerTriggerInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public Integer getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(Integer schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
        this.repeatCount = repeatCount;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getStopTime() {
        return stopTime;
    }

    public void setStopTime(Timestamp stopTime) {
        this.stopTime = stopTime;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
