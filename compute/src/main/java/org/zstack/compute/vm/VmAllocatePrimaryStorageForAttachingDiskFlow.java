package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeInventory;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocatePrimaryStorageForAttachingDiskFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VolumeInventory volume = (VolumeInventory) data.get(VmInstanceConstant.Params.AttachingVolumeInventory.toString());
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        String hostUuid = spec.getVmInventory().getHostUuid() == null ? spec.getVmInventory().getLastHostUuid() : spec.getVmInventory().getHostUuid();
        assert hostUuid != null;
        HostVO hvo = dbf.findByUuid(hostUuid, HostVO.class);
        HostInventory hinv = HostInventory.valueOf(hvo);
        spec.setDestHost(hinv);

        AllocatePrimaryStorageMsg msg = new AllocatePrimaryStorageMsg();
        msg.setSize(volume.getSize());
        msg.setPurpose(PrimaryStorageAllocationPurpose.CreateVolume.toString());
        msg.setRequiredHostUuid(hinv.getUuid());
        msg.setDiskOfferingUuid(volume.getDiskOfferingUuid());
        msg.setServiceId(bus.makeLocalServiceId(PrimaryStorageConstant.SERVICE_ID));
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AllocatePrimaryStorageReply ar = (AllocatePrimaryStorageReply)reply;
                    data.put(VmInstanceConstant.Params.DestPrimaryStorageInventoryForAttachingVolume.toString(), ar.getPrimaryStorageInventory());
                    data.put(VmAllocatePrimaryStorageForAttachingDiskFlow.class, ar.getSize());
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        Long size = (Long) data.get(VmAllocatePrimaryStorageForAttachingDiskFlow.class);
        if (size != null) {
            PrimaryStorageInventory pri = (PrimaryStorageInventory) data.get(VmInstanceConstant.Params.DestPrimaryStorageInventoryForAttachingVolume.toString());
            ReturnPrimaryStorageCapacityMsg rmsg = new ReturnPrimaryStorageCapacityMsg();
            rmsg.setPrimaryStorageUuid(pri.getUuid());
            rmsg.setDiskSize(size);
            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, pri.getUuid());
            bus.send(rmsg);
        }
        chain.rollback();
    }
}
