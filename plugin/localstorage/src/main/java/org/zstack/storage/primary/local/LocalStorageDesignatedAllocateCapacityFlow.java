package org.zstack.storage.primary.local;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.core.db.SQL;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.asyncbatch.AsyncLoop;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
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
import org.zstack.utils.function.Function;
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
        List<AllocatePrimaryStorageMsg> msgs = new ArrayList<>();

        ErrorCode errorCode = checkIfSpecifyPrimaryStorage(spec);
        if(errorCode != null){
            trigger.fail(errorCode);
            return;
        }

        AllocatePrimaryStorageMsg rootVolumeAllocationMsg =getRootVolumeAllocationMsg(spec);
        msgs.add(rootVolumeAllocationMsg);
        List<AllocatePrimaryStorageMsg> dataVolumeAllocationMsgs = getDataVolumeAllocationMsgs(spec);
        msgs.addAll(dataVolumeAllocationMsgs);

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

                        AllocatePrimaryStorageReply ar = reply.castReply();
                        VolumeSpec vspec = new VolumeSpec();

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

                        //if requiredps of root/data is local ,pre-reserve host avaliable capacity
                        if (ar.getPrimaryStorageInventory().getUuid() != null) {
                            String psType = Q.New(PrimaryStorageVO.class)
                                    .select(PrimaryStorageVO_.type)
                                    .eq(PrimaryStorageVO_.uuid, spec.getRequiredPrimaryStorageUuidForRootVolume())
                                    .findValue();

                            PrimaryStorageVO ps = dbf.findByUuid(spec.getRequiredPrimaryStorageUuidForRootVolume(), PrimaryStorageVO.class);
                            LocalStorageReserveHostCapacityMsg msg = new LocalStorageReserveHostCapacityMsg();
                            msg.setHostUuid(spec.getDestHost().getUuid());
                            msg.setSize(ar.getSize());
                            msg.setPrimaryStorageUuid(ar.getPrimaryStorageInventory().getUuid());
                            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ar.getPrimaryStorageInventory().getUuid());
                            bus.send(msg, new CloudBusCallBack(completion) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        completion.fail(reply.getError());
                                    } else {
                                        vspec.setIsPreReservedOnLocal(true);
                                        completion.success();
                                    }
                                }
                            });

                            spec.getVolumeSpecs().add(vspec);
                        }
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

    private void reserveStorageCapacityOnHost(String dstHostUuid, String psUuid, Long size, final Completion completion) {
        LocalStorageReserveHostCapacityMsg msg = new LocalStorageReserveHostCapacityMsg();
        msg.setHostUuid(dstHostUuid);
        msg.setSize(size);
        msg.setPrimaryStorageUuid(psUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, psUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    completion.fail(reply.getError());
                } else {
                    completion.success();
                }
            }
        });
    }

    private void returnStorageCapacityToHost(final String dstHostUuid, final String primaryStorageUuid, final Long size) {
        LocalStorageReturnHostCapacityMsg msg = new LocalStorageReturnHostCapacityMsg();
        msg.setHostUuid(dstHostUuid);
        msg.setSize(size);
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    //TODO recalculate host capacity
                    logger.warn(String.format("failed to return capacity[%s] to the host[uuid:%s] of local storage[uuid:%s], %s",
                            size, dstHostUuid, primaryStorageUuid, reply.getError()));
                }
            }
        });
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

    private AllocatePrimaryStorageMsg getRootVolumeAllocationMsg(VmInstanceSpec spec){

        String bsType = Q.New(BackupStorageVO.class)
                .select(BackupStorageVO_.type)
                .eq(BackupStorageVO_.uuid,spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid())
                .findValue();
        List<String> primaryStorageTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(bsType);
        DebugUtils.Assert(primaryStorageTypes != null, "why primaryStorageTypes is null");

        AllocatePrimaryStorageMsg rmsg = new AllocatePrimaryStorageMsg();
        rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        if (spec.getImageSpec() != null) {
            rmsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
        }
        rmsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForRootVolume());
        rmsg.setRequiredHostUuid(spec.getDestHost().getUuid());
        if (ImageMediaType.ISO.toString().equals(spec.getImageSpec().getInventory().getMediaType())) {
            rmsg.setSize(spec.getRootDiskOffering().getDiskSize());
            rmsg.setDiskOfferingUuid(spec.getRootDiskOffering().getUuid());
        } else {
            //TODO: find a way to allow specifying strategy for root disk
            rmsg.setSize(spec.getImageSpec().getInventory().getSize());
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

    private  List<AllocatePrimaryStorageMsg> getDataVolumeAllocationMsgs(VmInstanceSpec spec){
        List<AllocatePrimaryStorageMsg> msgs = new ArrayList<>();

        if (spec.getDataDiskOfferings() == null || spec.getDataDiskOfferings().isEmpty()) {
            return msgs;
        }

        String bsType = Q.New(BackupStorageVO.class)
                .select(BackupStorageVO_.type)
                .eq(BackupStorageVO_.uuid,spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid())
                .findValue();
        List<String> primaryStorageTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(bsType);

        for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
            AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
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

        List<IncreasePrimaryStorageCapacityMsg> msgs = CollectionUtils.transformToList(spec.getVolumeSpecs(), new Function<IncreasePrimaryStorageCapacityMsg, VolumeSpec>() {
            @Override
            public IncreasePrimaryStorageCapacityMsg call(VolumeSpec arg) {
                if (arg.isVolumeCreated()) {
                    // don't return capacity as it has been returned when the volume is deleted
                    return null;
                }

                IncreasePrimaryStorageCapacityMsg msg = new IncreasePrimaryStorageCapacityMsg();
                msg.setDiskSize(arg.getSize());
                msg.setPrimaryStorageUuid(arg.getPrimaryStorageInventory().getUuid());
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, arg.getPrimaryStorageInventory().getUrl());
                return msg;
            }
        });

        bus.send(msgs);

        //rollback local storage host capacity
        spec.getVolumeSpecs().forEach(VolumeSpec -> {
            if (VolumeSpec.isPreReservedOnLocal()) {
                returnStorageCapacityToHost(spec.getDestHost().getUuid(), VolumeSpec.getPrimaryStorageInventory().getUuid(), VolumeSpec.getSize());
            }
        });

        trigger.rollback();
    }
}
