package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.volume.VolumeInventory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocatePrimaryStorageForAttachingDiskFlow implements Flow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;

    private String allocatedInstallUrl;

    @Override
    public void run(final FlowTrigger chain, final Map data) {
        final VolumeInventory volume = (VolumeInventory) data.get(VmInstanceConstant.Params.AttachingVolumeInventory.toString());
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        String hostUuid = spec.getVmInventory().getHostUuid() == null ? spec.getVmInventory().getLastHostUuid() : spec.getVmInventory().getHostUuid();

        if(hostUuid == null){
            ErrorCode errorCode = operr(" Can not find the vm's host, please start the vm[%s], then mount the disk", spec.getVmInventory().getUuid());
            chain.fail(errorCode);
            return;
        }
        HostVO hvo = dbf.findByUuid(hostUuid, HostVO.class);
        HostInventory hinv = HostInventory.valueOf(hvo);
        spec.setDestHost(hinv);

        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
        amsg.setSize(volume.getSize());
        amsg.setPurpose(PrimaryStorageAllocationPurpose.CreateDataVolume.toString());
        amsg.setDiskOfferingUuid(volume.getDiskOfferingUuid());
        amsg.setServiceId(bus.makeLocalServiceId(PrimaryStorageConstant.SERVICE_ID));

        if (volume.isShareable()) {
            String clusterUuid = spec.getVmInventory().getClusterUuid();
            List<PrimaryStorageVO> vos = SQL.New("select pri" +
            " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                    " where ref.clusterUuid = :clusterUuid" +
                    " and ref.primaryStorageUuid = pri.uuid" +
                    " and pri.status = :status" +
                    " and pri.state = :priState")
                    .param("clusterUuid", clusterUuid)
                    .param("status", PrimaryStorageStatus.Connected)
                    .param("priState", PrimaryStorageState.Enabled)
                    .list();

            if (CollectionUtils.isNotEmpty(vos)) {
                amsg.setPossiblePrimaryStorageTypes(vos.stream()
                        .filter(v -> PrimaryStorageType.valueOf(v.getType()).isSupportSharedVolume())
                        .map(PrimaryStorageVO::getType)
                        .collect(Collectors.toList()));
            }

            amsg.setRequiredClusterUuids(Arrays.asList(clusterUuid));
        } else {
            amsg.setRequiredHostUuid(hinv.getUuid());
        }

        bus.send(amsg, new CloudBusCallBack(chain) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                    allocatedInstallUrl = ar.getAllocatedInstallUrl();
                    data.put(VmInstanceConstant.Params.DestPrimaryStorageInventoryForAttachingVolume.toString(), ar.getPrimaryStorageInventory());
                    data.put(VmInstanceConstant.Params.AllocatedUrlForAttachingVolume.toString(), allocatedInstallUrl);
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
            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
            rmsg.setAllocatedInstallUrl(allocatedInstallUrl);
            rmsg.setPrimaryStorageUuid(pri.getUuid());
            rmsg.setDiskSize(size);
            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, pri.getUuid());
            bus.send(rmsg);
        }
        chain.rollback();
    }
}
