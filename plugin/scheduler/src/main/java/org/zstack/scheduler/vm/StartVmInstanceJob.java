package org.zstack.scheduler.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.notification.N;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.StartVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.identity.AccountManager;
import org.zstack.scheduler.APICreateSchedulerJobMsg;
import org.zstack.scheduler.AbstractSchedulerJob;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Date;

/**
 * Created by root on 7/30/16.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StartVmInstanceJob extends AbstractSchedulerJob {
    private static final CLogger logger = Utils.getLogger(StartVmInstanceJob.class);
    @Autowired
    private transient AccountManager acntMgr;

    private String vmUuid;

    public StartVmInstanceJob(APICreateSchedulerJobMsg msg) {
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
        bus.send(smsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    N.New(VmInstanceVO.class, vmUuid).info_("Start vm instance job for vm[uuid:%s] succeed [executed time:%s]",
                            vmUuid, new Date().toString());
                } else {
                    N.New(VmInstanceVO.class, vmUuid).info_("Start vm instance job for vm[uuid:%s] failed [executed time:%s]",
                            vmUuid, new Date().toString());
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
