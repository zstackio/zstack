package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.scheduler.SchedulerFacadeImpl;
import org.zstack.core.scheduler.SchedulerJob;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.APIStartVmInstanceEvent;
import org.zstack.header.vm.StartVmInstanceMsg;
import org.zstack.header.vm.StartVmInstanceReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by root on 7/30/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StartVmInstanceJob implements SchedulerJob{
    @Autowired
    private transient CloudBus bus;
    @Autowired
    private transient AccountManager acntMgr;

    private Date startDate;
    private int interval;
    private int repeat;
    private String type;
    private String cron;
    private String schedulerName;
    private String jobName;
    private String jobGroup;
    private String triggerGroup;
    private String triggerName;
    private Timestamp createDate;

    private String vmUuid;
    private static final CLogger logger = Utils.getLogger(SchedulerFacadeImpl.class);
    public StartVmInstanceJob() {

    }
    public void run() {
        StartVmInstanceMsg smsg = new StartVmInstanceMsg();
        smsg.setVmInstanceUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, vmUuid);
        bus.send(smsg, new CloudBusCallBack() {
            @Override
            public void run(MessageReply reply) {
                APIStartVmInstanceEvent evt = new APIStartVmInstanceEvent(smsg.getId());
                if (reply.isSuccess()) {
                    StartVmInstanceReply creply = (StartVmInstanceReply) reply;
                    evt.setInventory(creply.getInventory());
                } else {
                    evt.setErrorCode(reply.getError());
                }
                bus.publish(evt);
            }
        });
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public int getInterval() {
        return interval;
    }

    public int getSchedulerInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    @Override
    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    @Override
    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    @Override
    public String getTriggerGroup() {
        return triggerGroup;
    }

    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
    }

    @Override
    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    @Override
    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
