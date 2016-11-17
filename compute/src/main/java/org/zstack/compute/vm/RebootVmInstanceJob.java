package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.scheduler.AbstractSchedulerJob;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.RebootVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by root on 8/16/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class RebootVmInstanceJob  extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(RebootVmInstanceJob.class);
    @Autowired
    private transient AccountManager acntMgr;

    private String vmUuid;

    public RebootVmInstanceJob(APICreateSchedulerMessage msg) {
        super(msg);
    }

    public RebootVmInstanceJob() {
        super();
    }

    public void run() {
        logger.debug(String.format("run scheduler for job: RebootVmInstanceJob; vm uuid is %s", vmUuid));
        RebootVmInstanceMsg rmsg = new RebootVmInstanceMsg();
        rmsg.setVmInstanceUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(rmsg, VmInstanceConstant.SERVICE_ID, vmUuid);
        bus.send(rmsg, new CloudBusCallBack() {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug(String.format("RebootVmInstanceJob for vm %s success", vmUuid));
                } else {
                    logger.debug(String.format("RebootVmInstanceJob for vm %s failed", vmUuid));
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
