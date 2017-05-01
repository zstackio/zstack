package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by frank on 11/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ValidateOperationFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(ValidateOperationFlow.class);

    @Autowired
    protected CloudBus bus;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec.getVmInventory().getRootVolumeUuid() == null) {
            // the vm is in an intermediate state that has no root volume
            trigger.next();
            return;
        }

        ValidateExpungeOperationMsg msg = new ValidateExpungeOperationMsg();
        msg.setClusterUuid(spec.getVmInventory().getClusterUuid());
        msg.setPrimaryStorageUuid(Q.New(VolumeVO.class)
                .eq(VolumeVO_.uuid, spec.getVmInventory().getRootVolumeUuid())
                .select(VolumeVO_.primaryStorageUuid)
                .findValue()
        );

        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
        bus.send(msg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to expunge the root volume[uuid:%s] of the vm[uuid:%s, name:%s], %s",
                            spec.getVmInventory().getRootVolumeUuid(), spec.getVmInventory().getUuid(),
                            spec.getVmInventory().getName(), reply.getError()));
                    trigger.fail(reply.getError());

                    return;
                }

                trigger.next();
            }
        });
    }
}
