package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.asyncbatch.AsyncLoop;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocatePrimaryStorageFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(VmAllocatePrimaryStorageFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    protected HostAllocatorManager hostAllocatorMgr;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        final List<AllocatePrimaryStorageMsg> msgs = new ArrayList<>();
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        HostInventory destHost = spec.getDestHost();
        final ImageInventory iminv = spec.getImageSpec().getInventory();

        // get ps types from image bs or cdroms bs
        List<String> primaryStorageTypes = selectPsTypesFromImageBSOrCdRomBs(spec);
        if (primaryStorageTypes.isEmpty()) {
            trigger.fail(operr("the primaryStorageTypes is empty"));
            return;
        }

        // allocate ps for root volume
        AllocatePrimaryStorageMsg rmsg = new AllocatePrimaryStorageMsg();
        rmsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForRootVolume());
        rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        if (spec.getImageSpec() != null) {
            rmsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
            Optional.ofNullable(spec.getImageSpec().getSelectedBackupStorage())
                    .ifPresent(it -> rmsg.setBackupStorageUuid(it.getBackupStorageUuid()));
        }
        if (ImageMediaType.ISO.toString().equals(iminv.getMediaType())) {
            rmsg.setSize(spec.getRootDiskOffering().getDiskSize());
            rmsg.setAllocationStrategy(spec.getRootDiskOffering().getAllocatorStrategy());
            rmsg.setDiskOfferingUuid(spec.getRootDiskOffering().getUuid());
        } else {
            //TODO: find a way to allow specifying strategy for root disk
            rmsg.setSize(iminv.getSize());
        }

        rmsg.setRequiredHostUuid(destHost.getUuid());
        rmsg.setPurpose(PrimaryStorageAllocationPurpose.CreateNewVm.toString());
        rmsg.setPossiblePrimaryStorageTypes(primaryStorageTypes);
        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
        msgs.add(rmsg);


        // allocate ps for data volumes
        for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
            amsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForDataVolume());
            amsg.setSize(dinv.getDiskSize());
            amsg.setRequiredHostUuid(destHost.getUuid());
            amsg.setAllocationStrategy(dinv.getAllocatorStrategy());
            amsg.setDiskOfferingUuid(dinv.getUuid());
            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
            msgs.add(amsg);
        }

        new AsyncLoop<AllocatePrimaryStorageMsg>(trigger) {
            @Override
            protected Collection<AllocatePrimaryStorageMsg> collectionForLoop() {
                return msgs;
            }

            @Override
            protected void run(AllocatePrimaryStorageMsg msg, Completion completion) {
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        VolumeSpec volumeSpec = new VolumeSpec();
                        AllocatePrimaryStorageReply ar = reply.castReply();
                        volumeSpec.setPrimaryStorageInventory(ar.getPrimaryStorageInventory());
                        volumeSpec.setSize(ar.getSize());
                        volumeSpec.setType(msg.getImageUuid() != null ? VolumeType.Root.toString() : VolumeType.Data.toString());
                        volumeSpec.setDiskOfferingUuid(msg.getDiskOfferingUuid());
                        if (VolumeType.Root.toString().equals(volumeSpec.getType())) {
                            volumeSpec.setTags(spec.getRootVolumeSystemTags());
                        } else {
                            volumeSpec.setTags(spec.getDataVolumeSystemTags());
                        }

                        spec.getVolumeSpecs().add(volumeSpec);

                        completion.success();
                    }
                });
            }

            @Override
            protected void done() {
                trigger.next();
            }

            @Override
            protected void error(ErrorCode errorCode) {
                trigger.fail(errorCode);
            }
        }.start();
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        for (VolumeSpec vspec : spec.getVolumeSpecs()) {
            if (vspec.isVolumeCreated()) {
                // don't return capacity as it has been returned when the volume is deleted
                continue;
            }

            IncreasePrimaryStorageCapacityMsg msg = new IncreasePrimaryStorageCapacityMsg();
            msg.setDiskSize(vspec.getSize());
            msg.setPrimaryStorageUuid(vspec.getPrimaryStorageInventory().getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vspec.getPrimaryStorageInventory().getUuid());
            bus.send(msg);
        }

        chain.rollback();
    }

    private List<String> selectPsTypesFromImageBSOrCdRomBs(final VmInstanceSpec vmSpec) {
        String imageBsType = findImageBsType(vmSpec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
        List<String> PsTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(imageBsType);
        List<VmInstanceSpec.CdRomSpec> cdRomsSpecs = vmSpec.getCdRomSpecs();
        for (VmInstanceSpec.CdRomSpec cdRom : cdRomsSpecs) {
            String cdRomBsType = findImageBsType(cdRom.getBackupStorageUuid());
            if (cdRomBsType != null) {
                PsTypes = (List<String>) CollectionUtils
                        .intersection(hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(cdRomBsType), PsTypes);
            }
        }
        return PsTypes;
    }

    private String findImageBsType(String bsUuid) {
        String type = Q.New(BackupStorageVO.class)
                .select(BackupStorageVO_.type)
                .eq(BackupStorageVO_.uuid, bsUuid)
                .findValue();
        return type;
    }
}
