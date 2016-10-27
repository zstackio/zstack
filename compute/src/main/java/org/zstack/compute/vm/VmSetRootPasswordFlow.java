package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.SetRootPasswordMsg;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.MigrateHostOverlayVolumeMsg;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

/**
 * Created by mingjian.deng on 16/10/26.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmSetRootPasswordFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmSetRootPasswordFlow.class);
    @Autowired
    private CloudBus bus;
    @Override
    public void run(final FlowTrigger trigger, Map data) {
        logger.debug("check if need reset rootpassword before start a new created vm.");
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if(spec.getAccountPerference() == null) {
            trigger.next();
            return;
        }

        if(spec.getDestHost() == null) {
            ErrorCode err = new ErrorCode(
                    "NO_DEST_HOST_FOUND", "not dest host found",
                    "not dest host found, can't send set root password cmd to the host!"
            );
            logger.warn(err.toString());
            trigger.next();
            return;
        }

        if(spec.getDestRootVolume() == null){
            ErrorCode err = new ErrorCode(
                    "NO_DEST_VOLUME_FOUND", "not dest root volume found",
                    "not dest root volume found, can't send set root password cmd to the host!"
            );
            logger.warn(err.toString());
            trigger.next();
            return;
        }

        SetRootPasswordMsg smsg = new SetRootPasswordMsg();
        smsg.setHostUuid(spec.getDestHost().getUuid());
        smsg.setVmAccountPerference(spec.getAccountPerference());
        smsg.setQcowFile(spec.getDestRootVolume().getInstallPath());
        bus.makeTargetServiceIdByResourceUuid(smsg, HostConstant.SERVICE_ID, spec.getAccountPerference().getVmUuid());

        MigrateHostOverlayVolumeMsg omsg = new MigrateHostOverlayVolumeMsg();
        omsg.setMessage(smsg);
        omsg.setVolumeUuid(spec.getDestRootVolume().getUuid());
        bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, spec.getDestRootVolume().getUuid());
        bus.send(omsg, new CloudBusCallBack(trigger) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    // set user-defined password failed should not stop the create procedure
                    logger.warn(String.format("there is a error while set root password: %s", reply.getError().toString()));
                }
                trigger.next();
            }
        });
    }

}
