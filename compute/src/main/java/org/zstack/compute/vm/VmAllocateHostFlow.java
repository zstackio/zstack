package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.progress.ProgressReportService.taskProgress;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocateHostFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected VmInstanceExtensionPointEmitter extEmitter;

    private long getTotalDataDiskSize(VmInstanceSpec spec) {
        long size = 0;
        for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
            size += dinv.getDiskSize();
        }
        return size;
    }

    protected AllocateHostMsg prepareMsg(VmInstanceSpec spec) {
        DesignatedAllocateHostMsg msg = new DesignatedAllocateHostMsg();

        List<DiskOfferingInventory> diskOfferings = new ArrayList<>();
        ImageInventory image = spec.getImageSpec().getInventory();
        long diskSize;
        if (image.getMediaType().equals(ImageMediaType.ISO.toString())) {
            DiskOfferingVO dvo = dbf.findByUuid(spec.getRootDiskOffering().getUuid(), DiskOfferingVO.class);
            diskSize = dvo.getDiskSize();
            diskOfferings.add(DiskOfferingInventory.valueOf(dvo));
        } else {
            diskSize = image.getSize();
        }
        diskSize += getTotalDataDiskSize(spec);
        diskOfferings.addAll(spec.getDataDiskOfferings());
        msg.setAutoScalingGroupUuid(spec.getAutoScalingGroupUuid());
        msg.setSoftAvoidHostUuids(spec.getSoftAvoidHostUuids());
        msg.setAvoidHostUuids(spec.getAvoidHostUuids());
        msg.setDiskOfferings(diskOfferings);
        msg.setDiskSize(diskSize);
        msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        List<L3NetworkInventory> l3Invs = VmNicSpec.getL3NetworkInventoryOfSpec(spec.getL3Networks());
        msg.setL3NetworkUuids(CollectionUtils.transformToList(l3Invs,
                new Function<String, L3NetworkInventory>() {
            @Override
            public String call(L3NetworkInventory arg) {
                return arg.getUuid();
            }
        }));
        msg.setImage(image);
        msg.setVmOperation(spec.getCurrentVmOperation().toString());

        if (spec.getVmInventory().getZoneUuid() != null) {
            msg.setZoneUuid(spec.getVmInventory().getZoneUuid());
        }
        if (spec.getVmInventory().getClusterUuid() != null) {
            msg.setClusterUuid(spec.getVmInventory().getClusterUuid());
        }
        msg.setHostUuid(spec.getRequiredHostUuid());
        if (spec.getHostAllocatorStrategy() != null) {
            msg.setAllocatorStrategy(spec.getHostAllocatorStrategy());
        } else {
            msg.setAllocatorStrategy(spec.getVmInventory().getAllocatorStrategy());
        }
        if (spec.getRequiredPrimaryStorageUuidForRootVolume() != null) {
            msg.addRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForRootVolume());
        }
        if (spec.getRequiredPrimaryStorageUuidForDataVolume() != null) {
            msg.addRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForDataVolume());
        }
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        msg.setVmInstance(spec.getVmInventory());

        if (spec.getImageSpec().getSelectedBackupStorage() != null) {
            msg.setRequiredBackupStorageUuid(spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
        }
        return msg;
    }

    @Override
    public void run(final FlowTrigger chain, Map data) {
        taskProgress("allocate candidate hosts");

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        if (VmOperation.NewCreate != spec.getCurrentVmOperation()
                && VmOperation.ChangeImage != spec.getCurrentVmOperation()) {
            throw new CloudRuntimeException("VmAllocateHostFlow is only for creating new VM or changing image");
        }

        if (VmOperation.ChangeImage == spec.getCurrentVmOperation() && spec.getDestHost() != null) {
            logger.debug(String.format("changing image for vm[uuid:%s] and spec.getDestHost() != null, " +
                    "so skip VmAllocateHostFlow", spec.getVmInventory().getUuid()));
            chain.next();
            return;
        }

        AllocateHostMsg msg = this.prepareMsg(spec);

        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AllocateHostReply areply = (AllocateHostReply) reply;
                    spec.setDestHost(areply.getHost());

                    // the vm instance will still be stopped after ChangeImage
                    if (spec.getCurrentVmOperation() == VmOperation.ChangeImage) {
                        ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
                        msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
                        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
                        msg.setHostUuid(spec.getDestHost().getUuid());
                        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
                        bus.send(msg);

                        chain.next();
                        return;
                    }

                    // update the vm's host uuid and hypervisor type so even if the management node died later and the vm's state
                    // is stuck in Starting, we know which host it's created on and can check its state on the host
                    String oldHostUuid = spec.getVmInventory().getHostUuid() != null ?
                            spec.getVmInventory().getHostUuid() : spec.getVmInventory().getLastHostUuid();
                    oldHostUuid = dbf.isExist(oldHostUuid, HostVO.class) ? oldHostUuid : null;
                    SQL.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, spec.getVmInventory().getUuid())
                            .set(VmInstanceVO_.clusterUuid, spec.getDestHost().getClusterUuid())
                            .set(VmInstanceVO_.lastHostUuid, oldHostUuid)
                            .set(VmInstanceVO_.hostUuid, spec.getDestHost().getUuid())
                            .set(VmInstanceVO_.hypervisorType, spec.getDestHost().getHypervisorType())
                            .update();
                    chain.next();
                } else {
                    chain.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        HostInventory host = spec.getDestHost();

        // if ChangeImage, then no need to ReturnHostCapacity, and resume vm info
        if (spec.getCurrentVmOperation() == VmOperation.ChangeImage) {
            VmInstanceVO vmvo = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
            vmvo.setClusterUuid(spec.getVmInventory().getClusterUuid());
            vmvo.setLastHostUuid(spec.getVmInventory().getLastHostUuid());
            vmvo.setHypervisorType(spec.getVmInventory().getHypervisorType());
            dbf.update(vmvo);
        } else if (host != null) {
            ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
            msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
            msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
            msg.setHostUuid(host.getUuid());
            msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
            bus.send(msg);
        }

        extEmitter.cleanUpAfterVmFailedToStart(spec.getVmInventory());
        chain.rollback();
    }
}
