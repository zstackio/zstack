package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.compute.vm.VmAllocatePrimaryStorageFlow;
import org.zstack.core.asyncbatch.AsyncLoop;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * Created by lining on 2017/09/29.
 *
 * Support Scene:
 *  local + nfs(smp)
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageDesignatedAllocateCapacityFlow implements Flow {
    private static final CLogger logger = Utils.getLogger(LocalStorageDesignatedAllocateCapacityFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected HostAllocatorManager hostAllocatorMgr;

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<AllocatePrimaryStorageSpaceMsg> msgs = new ArrayList<>();

        ErrorCode errorCode = checkIfSpecifyPrimaryStorage(spec);
        if(errorCode != null){
            trigger.fail(errorCode);
            return;
        }

        AllocatePrimaryStorageSpaceMsg rootVolumeAllocationMsg = getRootVolumeAllocationMsg(spec);
        msgs.add(rootVolumeAllocationMsg);
        List<AllocatePrimaryStorageSpaceMsg> dataVolumeAllocationMsgs = getDataVolumeAllocationMsgs(spec);
        msgs.addAll(dataVolumeAllocationMsgs);

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

                        AllocatePrimaryStorageSpaceReply ar = reply.castReply();
                        VolumeSpec vspec = new VolumeSpec();
                        vspec.setAllocatedInstallUrl(ar.getAllocatedInstallUrl());
                        if (msg == rootVolumeAllocationMsg) {
                            vspec.setSize(ar.getSize());
                            vspec.setPrimaryStorageInventory(ar.getPrimaryStorageInventory());
                            vspec.setType(VolumeType.Root.toString());
                        } else {
                            vspec.setSize(ar.getSize());
                            vspec.setPrimaryStorageInventory(ar.getPrimaryStorageInventory());
                            vspec.setDiskOfferingUuid(msg.getDiskOfferingUuid());
                            vspec.setType(VolumeType.Data.toString());
                        }
                        spec.getVolumeSpecs().add(vspec);

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

    private ErrorCode checkIfSpecifyPrimaryStorage(VmInstanceSpec spec){
        if(spec.getRequiredPrimaryStorageUuidForRootVolume() == null){
            ErrorCode errorCode = operr("The cluster[uuid=%s] mounts multiple primary storage[LocalStorage, other non-LocalStorage primary storage], You must specify the primary storage where the root disk is located",
                    spec.getDestHost().getClusterUuid());
            return errorCode;
        }

        if(spec.getDataDiskOfferings() != null && !spec.getDataDiskOfferings().isEmpty() && spec.getRequiredPrimaryStorageUuidForDataVolume() == null){
            ErrorCode errorCode = operr("The cluster[uuid=%s] mounts multiple primary storage[LocalStorage, other non-LocalStorage primary storage], You must specify the primary storage where the data disk is located",
                    spec.getDestHost().getClusterUuid());
            return errorCode;
        }

        return null;
    }

    private AllocatePrimaryStorageSpaceMsg getRootVolumeAllocationMsg(VmInstanceSpec spec){
        List<String> primaryStorageTypes = null;
        if (spec.getImageSpec().isNeedDownload() || spec.getImageSpec().getSelectedBackupStorage() != null) {
            SimpleQuery<BackupStorageVO> q = dbf.createQuery(BackupStorageVO.class);
            q.select(BackupStorageVO_.type);
            q.add(BackupStorageVO_.uuid, SimpleQuery.Op.EQ, spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
            String bsType = q.findValue();
            primaryStorageTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(bsType);
            DebugUtils.Assert(primaryStorageTypes != null, "why primaryStorageTypes is null");
        }

        AllocatePrimaryStorageSpaceMsg rmsg = new AllocatePrimaryStorageSpaceMsg();
        rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        if (spec.getImageSpec() != null) {
            rmsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
        }
        rmsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForRootVolume());
        rmsg.setRequiredHostUuid(spec.getDestHost().getUuid());
        rmsg.setSize(spec.getRootDiskAllocateSize());
        if (spec.getRootDiskOffering() != null) {
            rmsg.setDiskOfferingUuid(spec.getRootDiskOffering().getUuid());
        }

        if (spec.getCurrentVmOperation() == VmOperation.NewCreate) {
            rmsg.setPurpose(PrimaryStorageAllocationPurpose.CreateNewVm.toString());
        } else if (spec.getCurrentVmOperation() == VmOperation.AttachVolume) {
            rmsg.setPurpose(PrimaryStorageAllocationPurpose.CreateVolume.toString());
        }

        String requiredPrimaryStorageType = Q.New(PrimaryStorageVO.class)
                .select(PrimaryStorageVO_.type)
                .eq(PrimaryStorageVO_.uuid, spec.getRequiredPrimaryStorageUuidForRootVolume())
                .findValue();
        if(LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(requiredPrimaryStorageType)){
            rmsg.setAllocationStrategy(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);
        }

        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);

        rmsg.setPossiblePrimaryStorageTypes(primaryStorageTypes);
        return rmsg;
    }

    private  List<AllocatePrimaryStorageSpaceMsg> getDataVolumeAllocationMsgs(VmInstanceSpec spec){
        List<AllocatePrimaryStorageSpaceMsg> msgs = new ArrayList<>();

        if (spec.getDataDiskOfferings() == null || spec.getDataDiskOfferings().isEmpty()) {
            return msgs;
        }

        String bsType = Q.New(BackupStorageVO.class)
                .select(BackupStorageVO_.type)
                .eq(BackupStorageVO_.uuid,spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid())
                .findValue();
        List<String> primaryStorageTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(bsType);

        for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
            AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
            amsg.setSize(dinv.getDiskSize());
            amsg.setRequiredHostUuid(spec.getDestHost().getUuid());
            amsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForDataVolume());

            String requiredPrimaryStorageType = Q.New(PrimaryStorageVO.class)
                    .select(PrimaryStorageVO_.type)
                    .eq(PrimaryStorageVO_.uuid, spec.getRequiredPrimaryStorageUuidForDataVolume())
                    .findValue();
            if(LocalStorageConstants.LOCAL_STORAGE_TYPE.equals(requiredPrimaryStorageType)){
                amsg.setAllocationStrategy(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);
            }

            amsg.setPossiblePrimaryStorageTypes(primaryStorageTypes);
            amsg.setDiskOfferingUuid(dinv.getUuid());
            if (spec.getImageSpec() != null) {
                amsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
            }
            bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
            msgs.add(amsg);
        }

        return msgs;
    }

    @Override
    public void rollback(FlowRollback trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        if (spec.getVolumeSpecs().isEmpty()) {
            trigger.rollback();
            return;
        }

        List<ReleasePrimaryStorageSpaceMsg> msgs = CollectionUtils.transformToList(spec.getVolumeSpecs(), new Function<ReleasePrimaryStorageSpaceMsg, VolumeSpec>() {
            @Override
            public ReleasePrimaryStorageSpaceMsg call(VolumeSpec arg) {
                if (arg.isVolumeCreated()) {
                    // don't return capacity as it has been returned when the volume is deleted
                    return null;
                }

                ReleasePrimaryStorageSpaceMsg msg = new ReleasePrimaryStorageSpaceMsg();
                msg.setAllocatedInstallUrl(arg.getAllocatedInstallUrl());
                msg.setDiskSize(arg.getSize());
                msg.setPrimaryStorageUuid(arg.getPrimaryStorageInventory().getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, arg.getPrimaryStorageInventory().getUrl());
                return msg;
            }
        });

        spec.getVolumeSpecs().clear();
        bus.send(msgs);
        trigger.rollback();
    }
}
