package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.scheduler.AbstractSchedulerJob;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.StartVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by root on 7/30/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StartVmInstanceJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(StartVmInstanceJob.class);
    @Autowired
    private transient AccountManager acntMgr;

    private String vmUuid;

    public StartVmInstanceJob(APICreateSchedulerMessage msg) {
        super(msg);
    }

    public StartVmInstanceJob() {
        super();
    }

    public void run() {
        logger.debug(String.format("run scheduler for job: StartVmInstanceJob; vm uuid is %s", vmUuid));
        StartVmInstanceMsg smsg = new StartVmInstanceMsg();
        smsg.setVmInstanceUuid(vmUuid);
        smsg.setAccountUuid(getAccountUuid());
        bus.makeTargetServiceIdByResourceUuid(smsg, VmInstanceConstant.SERVICE_ID, vmUuid);
        bus.send(smsg, new CloudBusCallBack() {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug(String.format("StartVmInstanceJob for vm %s success", vmUuid));
                } else {
                    logger.debug(String.format("StartVmInstanceJob for vm %s failed", vmUuid));
                }
            }
        });
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

}
