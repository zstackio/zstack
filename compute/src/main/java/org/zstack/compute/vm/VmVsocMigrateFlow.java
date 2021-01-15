package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.host.HostSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

import static org.zstack.core.CoreGlobalProperty.PLATFORM_ID;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmVsocMigrateFlow implements Flow {
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    private static final CLogger logger = Utils.getLogger(VmVsocMigrateFlow.class);

    @Override
    public void run(FlowTrigger chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        VmVsocMigrateMsg msg = new VmVsocMigrateMsg();
        msg.setMigrateType(VmInstanceConstant.HOT_MIGRATE);
        msg.setDestSocId(HostSystemTags.HOST_SSCARDID.getTag(spec.getDestHost().getUuid(), HostVO.class));
        msg.setVmUuid(spec.getVmInventory().getUuid());
        msg.setUuid(spec.getVmInventory().getHostUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, spec.getVmInventory().getHostUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });

    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        DeleteVmVsocFileMsg msgDelete = new DeleteVmVsocFileMsg();
        msgDelete.setVmInstanceUuid(spec.getVmInventory().getUuid());
        msgDelete.setHostUuid(spec.getDestHost().getUuid());
        msgDelete.setPlatformId(PLATFORM_ID);
        bus.makeTargetServiceIdByResourceUuid(msgDelete, HostConstant.SERVICE_ID, spec.getDestHost().getUuid());
        bus.send(msgDelete, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug("SUCESSS: call vsoc_delete");
                } else {
                    logger.error(String.format("FAIL: call vsoc_delete, because: %s", reply.getError().toString()));
                }
            }
        });

        CreateVmVsocFileMsg msgCreate = new CreateVmVsocFileMsg();
        msgCreate.setVmInstanceUuid(spec.getVmInventory().getUuid());
        msgCreate.setHostUuid(spec.getDestHost().getUuid());
        msgCreate.setPlatformId(PLATFORM_ID);
        bus.makeTargetServiceIdByResourceUuid(msgCreate, HostConstant.SERVICE_ID, spec.getVmInventory().getHostUuid());
        bus.send(msgDelete, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    logger.debug("SUCESSS: call vsoc_create");
                } else {
                    logger.error(String.format("FAIL: call vsoc_create, because: %s", reply.getError().toString()));
                }
            }
        });

        trigger.rollback();
    }
}
