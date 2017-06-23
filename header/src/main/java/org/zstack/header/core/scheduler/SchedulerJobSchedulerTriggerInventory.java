package org.zstack.header.core.scheduler;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by AlanJager on 2017/6/8.
 */
@Inventory(mappingVOClass = SchedulerJobSchedulerTriggerRefVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "trigger", inventoryClass = SchedulerTriggerInventory.class,
                foreignKey = "schedulerTriggerUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "job", inventoryClass = SchedulerJobInventory.class,
                foreignKey = "schedulerJobUuid", expandedInventoryKey = "uuid")
})
public class SchedulerJobSchedulerTriggerInventory {
    private String uuid;
    private String schedulerJobUuid;
    private String schedulerTriggerUuid;
    private String jobGroup;
    private String triggerGroup;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected SchedulerJobSchedulerTriggerInventory(SchedulerJobSchedulerTriggerRefVO vo) {
        this.setUuid(vo.getUuid());
        this.setSchedulerJobUuid(vo.getSchedulerJobUuid());
        this.setSchedulerTriggerUuid(vo.getSchedulerTriggerUuid());
        this.setJobGroup(vo.getJobGroup());
        this.setTriggerGroup(vo.getTriggerGroup());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static SchedulerJobSchedulerTriggerInventory valueOf(SchedulerJobSchedulerTriggerRefVO vo) {
        return new SchedulerJobSchedulerTriggerInventory(vo);
    }

    public static List<SchedulerJobSchedulerTriggerInventory> valueOf(Collection<SchedulerJobSchedulerTriggerRefVO> vos) {
        List<SchedulerJobSchedulerTriggerInventory> invs = new ArrayList<SchedulerJobSchedulerTriggerInventory>(vos.size());
        for (SchedulerJobSchedulerTriggerRefVO vo : vos) {
            invs.add(SchedulerJobSchedulerTriggerInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSchedulerJobUuid() {
        return schedulerJobUuid;
    }

    public void setSchedulerJobUuid(String schedulerJobUuid) {
        this.schedulerJobUuid = schedulerJobUuid;
    }

    public String getSchedulerTriggerUuid() {
        return schedulerTriggerUuid;
    }

    public void setSchedulerTriggerUuid(String schedulerTriggerUuid) {
        this.schedulerTriggerUuid = schedulerTriggerUuid;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getTriggerGroup() {
        return triggerGroup;
    }

    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
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
