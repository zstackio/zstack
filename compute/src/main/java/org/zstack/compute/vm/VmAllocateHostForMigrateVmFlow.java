package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.AllocateHostReply;
import org.zstack.header.allocator.DesignatedAllocateHostMsg;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.ReturnHostCapacityMsg;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostForMigrateVmFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;

    private static final String SUCCESS = VmAllocateHostForMigrateVmFlow.class.getName();

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        String destHostUuid = null;
        DesignatedAllocateHostMsg msg = new DesignatedAllocateHostMsg();
        if (spec.getMessage() instanceof MigrateVmMessage) {
            destHostUuid = ((MigrateVmMessage)spec.getMessage()).getHostUuid();
            msg.setAllowNoL3Networks(true);
        }

        msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        msg.setHostUuid(destHostUuid);
        msg.getAvoidHostUuids().add(spec.getVmInventory().getHostUuid());
        msg.setAllocationScene(spec.getAllocationScene());
        if (spec.getMessage() != null && spec.getMessage() instanceof MigrateVmMsg) {
            MigrateVmMsg migrateVmMsg = (MigrateVmMsg) spec.getMessage();
            if (migrateVmMsg.getAvoidHostUuids() != null) {
                msg.getAvoidHostUuids().addAll(migrateVmMsg.getAvoidHostUuids());
            }
        }
        if (spec.getImageSpec() != null) {
            msg.setImage(spec.getImageSpec().getInventory());
        }
        msg.setVmInstance(spec.getVmInventory());
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        msg.setAllocatorStrategy(HostAllocatorConstant.MIGRATE_VM_ALLOCATOR_TYPE);
        msg.setVmOperation(spec.getCurrentVmOperation().toString());
        msg.setRequiredPrimaryStorageUuids(spec.getVmInventory().getAllVolumes().stream()
                .map(VolumeInventory::getPrimaryStorageUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        msg.setL3NetworkUuids(CollectionUtils.transformToList(
                VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()), new Function<String, L3NetworkInventory>() {
            @Override
            public String call(L3NetworkInventory arg) {
                return arg.getUuid();
            }
        }));
        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AllocateHostReply ar = (AllocateHostReply)reply;
                    spec.setDestHost(ar.getHost());
                    data.put(SUCCESS, true);
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        if (data.containsKey(SUCCESS)) {
            final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
            ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
            msg.setHostUuid(spec.getDestHost().getUuid());
            msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
            msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
            bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
            bus.send(msg);
        }
        chain.rollback();
    }
}
