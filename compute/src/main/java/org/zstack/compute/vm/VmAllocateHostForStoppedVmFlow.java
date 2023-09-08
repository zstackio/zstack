package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.Map;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostForStoppedVmFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    private static final String SUCCESS = VmAllocateHostForStoppedVmFlow.class.getName();

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        AllocateHostMsg amsg;

        DesignatedAllocateHostMsg msg = new DesignatedAllocateHostMsg();
        msg.setVmInstance(spec.getVmInventory());
        msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        msg.setVmOperation(spec.getCurrentVmOperation().toString());
        if (spec.getImageSpec() != null) {
            msg.setImage(spec.getImageSpec().getInventory());
        }
        if ((spec.getRequiredClusterUuid() != null &&
                !spec.getRequiredClusterUuid().equals(msg.getVmInstance().getClusterUuid()))
                || spec.getRequiredHostUuid() != null || CollectionUtils.isEmpty(spec.getVmInventory().getVmNics())) {
            msg.setAllocatorStrategy(HostAllocatorConstant.DESIGNATED_HOST_ALLOCATOR_STRATEGY_TYPE);
            msg.setHostUuid(spec.getRequiredHostUuid());
        } else {
            String allocatorStrategy = spec.getVmInventory().getAllocatorStrategy();
            if (allocatorStrategy != null) {
                msg.setAllocatorStrategy(allocatorStrategy);
            } else {
                msg.setAllocatorStrategy(HostAllocatorConstant.LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE);
            }
        }
        if (CollectionUtils.isEmpty(spec.getVmInventory().getVmNics())) {
            msg.setAllowNoL3Networks(true);
        }

        msg.setL3NetworkUuids(CollectionUtils.transformToList(VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()),
                new Function<String, L3NetworkInventory>() {
            @Override
            public String call(L3NetworkInventory arg) {
                return arg.getUuid();
            }
        }));
        if (spec.getRequiredClusterUuid() == null) {
            if (CollectionUtils.isEmpty(spec.getVmInventory().getVmNics())) {
                msg.setClusterUuid(spec.getVmInventory().getClusterUuid());
            }
        } else {
            msg.setClusterUuid(spec.getRequiredClusterUuid());
        }
        msg.setRequiredPrimaryStorageUuids(spec.getVmInventory().getAllVolumes().stream()
                .map(VolumeInventory::getPrimaryStorageUuid)
                .collect(Collectors.toSet()));
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        msg.setSoftAvoidHostUuids(spec.getSoftAvoidHostUuids());
        msg.setAllocationScene(spec.getAllocationScene());
        msg.setAvoidHostUuids(spec.getAvoidHostUuids());
        amsg = msg;

        bus.send(amsg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    chain.fail(reply.getError());
                    return;
                }

                AllocateHostReply areply = (AllocateHostReply) reply;
                spec.setDestHost(areply.getHost());

                new SQLBatch(){
                    @Override
                    protected void scripts() {
                        String oldHostUuid = spec.getVmInventory().getHostUuid() == null ?
                                spec.getVmInventory().getLastHostUuid() : spec.getVmInventory().getHostUuid();
                        oldHostUuid = q(HostVO.class).eq(HostVO_.uuid, oldHostUuid).isExists() ? oldHostUuid : null;
                        sql(VmInstanceVO.class).eq(VmInstanceVO_.uuid, spec.getVmInventory().getUuid())
                                .set(VmInstanceVO_.lastHostUuid, oldHostUuid)
                                .set(VmInstanceVO_.hostUuid, areply.getHost().getUuid())
                                .set(VmInstanceVO_.clusterUuid, areply.getHost().getClusterUuid())
                                .update();
                    }
                }.execute();

                data.put(SUCCESS, true);
                chain.next();
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (data.containsKey(SUCCESS)) {
            VmInstanceVO vm = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
            vm.setHostUuid(null);
            dbf.update(vm);

            HostInventory host = spec.getDestHost();
            ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
            msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
            msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
            msg.setHostUuid(host.getUuid());
            msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
            bus.send(msg);

            extEmitter.cleanUpAfterVmFailedToStart(spec.getVmInventory());
        }
        chain.rollback();
    }
}
