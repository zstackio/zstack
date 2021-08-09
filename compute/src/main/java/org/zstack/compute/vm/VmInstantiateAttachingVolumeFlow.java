package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.InstantiateVolumeMsg;
import org.zstack.header.volume.InstantiateVolumeReply;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmInstantiateAttachingVolumeFlow extends NoRollbackFlow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected EventFacade evtf;

    @Override
    public void run(final FlowTrigger chain, final Map ctx) {
        final VolumeInventory volume = (VolumeInventory) ctx.get(VmInstanceConstant.Params.AttachingVolumeInventory.toString());
        final VmInstanceSpec spec = (VmInstanceSpec) ctx.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        assert volume != null;
        assert spec != null;

        final PrimaryStorageInventory pinv = (PrimaryStorageInventory) ctx.get(VmInstanceConstant.Params.DestPrimaryStorageInventoryForAttachingVolume.toString());

        InstantiateVolumeMsg msg = new InstantiateVolumeMsg();
        msg.setPrimaryStorageAllocated(true);
        msg.setPrimaryStorageUuid(pinv.getUuid());
        msg.setVolumeUuid(volume.getUuid());
        msg.setHostUuid(spec.getDestHost().getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volume.getUuid());
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    chain.fail(reply.getError());
                } else {
                    ctx.put(VmInstanceConstant.Params.AttachingVolumeInventory.toString(), ((InstantiateVolumeReply)reply).getVolume());
                    chain.next();
                }
            }
        });
    }
}
