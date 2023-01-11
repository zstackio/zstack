package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.asyncbatch.AsyncLoop;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
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
        final List<AllocatePrimaryStorageSpaceMsg> msgs = new ArrayList<>();
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        HostInventory destHost = spec.getDestHost();
        final ImageInventory iminv = spec.getImageSpec().getInventory();

        // allocate ps for root volume
        AllocatePrimaryStorageSpaceMsg rmsg = new AllocatePrimaryStorageSpaceMsg();
        rmsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForRootVolume());
        rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        if (spec.getImageSpec() != null) {
            rmsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
            Optional.ofNullable(spec.getImageSpec().getSelectedBackupStorage())
                    .ifPresent(it -> rmsg.setBackupStorageUuid(it.getBackupStorageUuid()));
        }
        rmsg.setSize(spec.getRootDiskAllocateSize());
        if (spec.getRootDiskOffering() != null) {
            rmsg.setDiskOfferingUuid(spec.getRootDiskOffering().getUuid());
            rmsg.setAllocationStrategy(spec.getRootDiskOffering().getAllocatorStrategy());
        }

        rmsg.setRequiredHostUuid(destHost.getUuid());
        rmsg.setPurpose(PrimaryStorageAllocationPurpose.CreateNewVm.toString());
        rmsg.setPossiblePrimaryStorageTypes(selectPsTypesFromSpec(spec));
        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
        msgs.add(rmsg);


        // allocate ps for data volumes
        for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
            AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
            amsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForDataVolume());
            amsg.setSize(dinv.getDiskSize());
            amsg.setRequiredHostUuid(destHost.getUuid());
            amsg.setAllocationStrategy(dinv.getAllocatorStrategy());
            amsg.setDiskOfferingUuid(dinv.getUuid());
            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
            msgs.add(amsg);
        }

        new AsyncLoop<AllocatePrimaryStorageSpaceMsg>(trigger) {
            @Override
            protected Collection<AllocatePrimaryStorageSpaceMsg> collectionForLoop() {
                return msgs;
            }

            @Override
            protected void run(AllocatePrimaryStorageSpaceMsg msg, Completion completion) {
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            completion.fail(reply.getError());
                            return;
                        }

                        VolumeSpec volumeSpec = new VolumeSpec();
                        AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                        volumeSpec.setAllocatedInstallUrl(ar.getAllocatedInstallUrl());
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

            ReleasePrimaryStorageSpaceMsg msg = new ReleasePrimaryStorageSpaceMsg();
            msg.setAllocatedInstallUrl(vspec.getAllocatedInstallUrl());
            msg.setDiskSize(vspec.getSize());
            msg.setPrimaryStorageUuid(vspec.getPrimaryStorageInventory().getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vspec.getPrimaryStorageInventory().getUuid());
            bus.send(msg);
        }

        spec.getVolumeSpecs().clear();
        chain.rollback();
    }

    private List<String> selectPsTypesFromSpec(final VmInstanceSpec spec) {
        // get ps types from image bs and cdroms bs
        List<String> psTypes = null;
        if (spec.getImageSpec().isNeedDownload() || spec.getImageSpec().getSelectedBackupStorage() != null) {
            String imageBsType = findImageBsType(spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
            psTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(imageBsType);
            DebugUtils.Assert(psTypes != null, "why primaryStorageTypes is null");
        }

        for (VmInstanceSpec.CdRomSpec cdRom : spec.getCdRomSpecs()) {
            if (!cdRom.isAttachedIso()) {
                continue;
            }

            String cdRomBsType = findImageBsType(cdRom.getBackupStorageUuid());
            if (psTypes == null) {
                psTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(cdRomBsType);
            } else {
                psTypes = new ArrayList<>(psTypes);
                psTypes.retainAll(hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(cdRomBsType));
            }
        }
        return psTypes;
    }

    private String findImageBsType(String bsUuid) {
        return Q.New(BackupStorageVO.class)
                .select(BackupStorageVO_.type)
                .eq(BackupStorageVO_.uuid, bsUuid)
                .findValue();
    }
}
