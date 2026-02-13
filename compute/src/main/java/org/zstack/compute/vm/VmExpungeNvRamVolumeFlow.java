package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.ExpungeVolumeMsg;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.multiErr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmExpungeNvRamVolumeFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmExpungeNvRamVolumeFlow.class);

    @Autowired
    protected CloudBus bus;

    @Override
    @SuppressWarnings("rawtypes")
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        String vmUuid = spec.getVmInventory().getUuid();

        List<VolumeVO> volumes = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .eq(VolumeVO_.type, VolumeType.NvRam)
                .list();
        if (volumes.isEmpty()) {
            trigger.next();
            return;
        }

        new While<>(volumes).each((vol, c) -> {
            ExpungeVolumeMsg msg = new ExpungeVolumeMsg();
            msg.setVolumeUuid(vol.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, vol.getUuid());
            bus.send(msg, new CloudBusCallBack(c) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to expunge the NvRam volume[uuid:%s] of the vm[uuid:%s, name:%s]: %s",
                                vol.getUuid(), spec.getVmInventory().getUuid(),
                                spec.getVmInventory().getName(), reply.getError()));

                        c.addError(reply.getError()
                                .withOpaque("volume.uuid", vol.getUuid()));
                    }

                    c.done();
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    trigger.fail(multiErr(errorCodeList.getCauses(), "failed to expunge the NvRam volumes"));
                    return;
                }

                trigger.next();
            }
        });
    }
}
