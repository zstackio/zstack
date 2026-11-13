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
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.allocator.HostAllocatorReserveExtensionPoint;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;

import java.util.Map;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostForMigrateVmFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateHostForMigrateVmFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

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
        if (spec.getImageSpec() != null && spec.getImageSpec().getInventory() != null) {
            msg.setImage(spec.getImageSpec().getInventory());
        }
        msg.setZoneUuid(spec.getVmInventory().getZoneUuid());
        msg.setVmInstance(spec.getVmInventory());
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        msg.setAllocatorStrategy(HostAllocatorConstant.MIGRATE_VM_ALLOCATOR_TYPE);
        msg.setVmOperation(VmInstanceConstant.VmOperation.Migrate.toString());
        msg.setMigrationMode(spec.getMigrationMode());
        msg.setRequiredPrimaryStorageUuids(spec.getVmInventory().getAllVolumes().stream()
                .map(VolumeInventory::getPrimaryStorageUuid)
                .collect(Collectors.toSet()));
        msg.setL3NetworkUuids(CollectionUtils.transformToList(
                VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks()), new Function<String, L3NetworkInventory>() {
            @Override
            public String call(L3NetworkInventory arg) {
                return arg.getUuid();
            }
        }));
        msg.setAllowNoL3Networks(true);
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
        if (!data.containsKey(SUCCESS)) {
            chain.rollback();
            return;
        }
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec.getMigrationMode() == VmMigrationMode.Live) {
            returnCapacity(spec, chain);
            return;
        }
        HostAllocatorSpec allocation = new HostAllocatorSpec();
        allocation.setVmInstance(spec.getVmInventory());
        allocation.setVmOperation(VmInstanceConstant.VmOperation.Migrate.toString());
        allocation.setMigrationMode(spec.getMigrationMode());
        new While<>(pluginRgty.getExtensionList(HostAllocatorReserveExtensionPoint.class))
                .each((ext, next) -> ext.releaseReservation(allocation, spec.getDestHost(), new Completion(next) {
                    @Override
                    public void success() {
                        next.done();
                    }

                    @Override
                    public void fail(ErrorCode error) {
                        logger.error(String.format("failed to release migration reservation for vm[%s] on host[%s], " +
                                "extension[%s]: %s", spec.getVmInventory().getUuid(), spec.getDestHost().getUuid(),
                                ext.getClass().getName(), error));
                        next.addError(error);
                        next.done();
                    }
                })).run(new WhileDoneCompletion(chain) {
                    @Override
                    public void done(ErrorCodeList errors) {
                        if (!errors.getCauses().isEmpty()) {
                            logger.error(String.format("migration reservation cleanup incomplete for vm[%s] " +
                                    "on host[%s]: %s", spec.getVmInventory().getUuid(),
                                    spec.getDestHost().getUuid(), errors));
                        }
                        returnCapacity(spec, chain);
                    }
                });
    }

    private void returnCapacity(VmInstanceSpec spec, FlowRollback chain) {
        ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
        msg.setHostUuid(spec.getDestHost().getUuid());
        msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
        bus.send(msg);
        chain.rollback();
    }
}
