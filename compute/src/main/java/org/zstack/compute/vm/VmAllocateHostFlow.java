package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.*;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO;
import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.progress.ProgressReportService.taskProgress;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateHostFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;

    private long getTotalDataDiskSize(VmInstanceSpec spec) {
        long size = 0;
        for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
            size += dinv.getDiskSize();
        }
        return size;
    }

    @Transactional
    private List<String> getAvoidHost(VmInstanceSpec spec){
        return Q.New(PrimaryStorageHostRefVO.class).select(PrimaryStorageHostRefVO_.hostUuid)
                .eq(PrimaryStorageHostRefVO_.primaryStorageUuid, spec.getRequiredPrimaryStorageUuidForRootVolume())
                .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                .listValues();
    }

    private AllocateHostMsg prepareMsg(Map<String, Object> ctx) {
        VmInstanceSpec spec = (VmInstanceSpec) ctx.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

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
        msg.setAvoidHostUuids(getAvoidHost(spec));
        msg.setDiskOfferings(diskOfferings);
        msg.setDiskSize(diskSize);
        msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
        msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
        msg.setL3NetworkUuids(CollectionUtils.transformToList(spec.getL3Networks(), new Function<String, L3NetworkInventory>() {
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
        if (spec.getVmInventory().getHostUuid() != null) {
            msg.setHostUuid(spec.getVmInventory().getHostUuid());
        }
        if (spec.getHostAllocatorStrategy() != null) {
            msg.setAllocatorStrategy(spec.getHostAllocatorStrategy());
        } else {
            msg.setAllocatorStrategy(spec.getVmInventory().getAllocatorStrategy());
        }
        if (spec.getRequiredPrimaryStorageUuidForRootVolume() != null) {
            msg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForRootVolume());
        }
        msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
        msg.setTimeout(TimeUnit.MINUTES.toMillis(60));
        msg.setVmInstance(spec.getVmInventory());
        msg.setRequiredBackupStorageUuid(spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
        return msg;
    }

    @Override
    public void run(final FlowTrigger chain, Map data) {
        taskProgress("allocate candidate hosts");

        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        if (VmOperation.NewCreate != spec.getCurrentVmOperation()) {
            throw new CloudRuntimeException("VmAllocateHostFlow is only for creating new VM");
        }

        AllocateHostMsg msg = this.prepareMsg(data);

        bus.send(msg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AllocateHostReply areply = (AllocateHostReply) reply;
                    spec.setDestHost(areply.getHost());

                    // update the vm's host uuid and hypervisor type so even if the management node died later and the vm's state
                    // is stuck in Starting, we know which host it's created on and can check its state on the host
                    VmInstanceVO vmvo = dbf.findByUuid(spec.getVmInventory().getUuid(), VmInstanceVO.class);
                    vmvo.setClusterUuid(spec.getDestHost().getClusterUuid());
                    vmvo.setLastHostUuid(vmvo.getHostUuid());
                    vmvo.setHostUuid(spec.getDestHost().getUuid());
                    vmvo.setHypervisorType(spec.getDestHost().getHypervisorType());
                    dbf.update(vmvo);

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
        if (host != null) {
            ReturnHostCapacityMsg msg = new ReturnHostCapacityMsg();
            msg.setCpuCapacity(spec.getVmInventory().getCpuNum());
            msg.setMemoryCapacity(spec.getVmInventory().getMemorySize());
            msg.setHostUuid(host.getUuid());
            msg.setServiceId(bus.makeLocalServiceId(HostAllocatorConstant.SERVICE_ID));
            bus.send(msg);
        }
        chain.rollback();
    }
}
