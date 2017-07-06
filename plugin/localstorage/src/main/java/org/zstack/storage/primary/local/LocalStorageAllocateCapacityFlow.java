package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.compute.allocator.HostAllocatorManager;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceSpec.VolumeSpec;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 7/2/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalStorageAllocateCapacityFlow implements Flow {
    private final static CLogger logger = Utils.getLogger(LocalStorageAllocateCapacityFlow.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected HostAllocatorManager hostAllocatorMgr;

    @Transactional(readOnly = true)
    private boolean isThereOtherNonLocalStoragePrimaryStorageForTheHost(String hostUuid, String localStorageUuid) {
        String sql = "select count(pri)" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref, HostVO host" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = host.clusterUuid" +
                " and host.uuid = :huuid" +
                " and pri.uuid != :puuid" +
                " and pri.type != :pstype";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("huuid", hostUuid);
        q.setParameter("puuid", localStorageUuid);
        q.setParameter("pstype", LocalStorageConstants.LOCAL_STORAGE_TYPE);
        return q.getSingleResult() > 0;
    }

    @Transactional(readOnly = true)
    private String getMostFreeLocalStorageUuid(String hostUuid) {
        String sql = "select ref.primaryStorageUuid" +
                " from LocalStorageHostRefVO ref, PrimaryStorageCapacityVO cap, PrimaryStorageVO pri" +
                " where cap.uuid = ref.primaryStorageUuid" +
                " and ref.primaryStorageUuid = pri.uuid" +
                " and pri.state = :state" +
                " and pri.status = :status" +
                " and ref.hostUuid = :huuid" +
                " group by ref.primaryStorageUuid" +
                " order by cap.availableCapacity desc";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("huuid", hostUuid);
        q.setParameter("state", PrimaryStorageState.Enabled);
        q.setParameter("status", PrimaryStorageStatus.Connected);
        List<String> result = q.getResultList();
        if(result.isEmpty()){
            String clusterUuid = Q.New(HostVO.class).select(HostVO_.clusterUuid)
                    .eq(HostVO_.uuid, hostUuid).findValue();
            throw new OperationFailureException(operr("There is no LocalStorage primary storage[state=%s,status=%s] on the cluster[%s]. Check the state/status of primary storage and make sure they have been attached to clusters"
                    , PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, clusterUuid));
        }
        return result.get(0);
    }

    @Transactional(readOnly = true)
    private String getRequiredStorageUuid(String hostUuid, String psUuid){
        if(psUuid == null){
            return getMostFreeLocalStorageUuid(hostUuid);
        }else if(Q.New(LocalStorageHostRefVO.class)
                .eq(LocalStorageHostRefVO_.hostUuid, hostUuid)
                .eq(LocalStorageHostRefVO_.primaryStorageUuid, psUuid)
                .isExists()){
            return psUuid;
        }else if(!Q.New(PrimaryStorageVO.class)
                .eq(PrimaryStorageVO_.uuid, psUuid)
                .eq(PrimaryStorageVO_.type, LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .isExists()){
            throw new OperationFailureException(argerr("the type of primary storage[uuid:%s] chosen is not local storage, " +
                    "check if the resource can be created on other storage when cluster has attached local primary storage", psUuid));
        }else {
            return getMostFreeLocalStorageUuid(hostUuid);
        }
    }

    @Override
    public void run(final FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());

        String localStorageUuid = getRequiredStorageUuid(spec.getDestHost().getUuid(), spec.getRequiredPrimaryStorageUuidForRootVolume());

        SimpleQuery<BackupStorageVO> bq = dbf.createQuery(BackupStorageVO.class);
        bq.select(BackupStorageVO_.type);
        bq.add(BackupStorageVO_.uuid, Op.EQ, spec.getImageSpec().getSelectedBackupStorage().getBackupStorageUuid());
        String bsType = bq.findValue();

        List<String> primaryStorageTypes = hostAllocatorMgr.getBackupStoragePrimaryStorageMetrics().get(bsType);
        DebugUtils.Assert(primaryStorageTypes != null, "why primaryStorageTypes is null");

        List<AllocatePrimaryStorageMsg> msgs = new ArrayList<>();

        AllocatePrimaryStorageMsg rmsg = new AllocatePrimaryStorageMsg();
        rmsg.setAllocationStrategy(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);
        rmsg.setVmInstanceUuid(spec.getVmInventory().getUuid());
        rmsg.setImageUuid(spec.getImageSpec().getInventory().getUuid());
        rmsg.setRequiredPrimaryStorageUuid(localStorageUuid);
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

        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);

        rmsg.setRequiredPrimaryStorageTypes(primaryStorageTypes);
        msgs.add(rmsg);

        if (!spec.getDataDiskOfferings().isEmpty()) {
            boolean hasOtherNonLocalStoragePrimaryStorage = isThereOtherNonLocalStoragePrimaryStorageForTheHost(
                    spec.getDestHost().getUuid(), localStorageUuid);

            for (DiskOfferingInventory dinv : spec.getDataDiskOfferings()) {
                AllocatePrimaryStorageMsg amsg = new AllocatePrimaryStorageMsg();
                amsg.setSize(dinv.getDiskSize());
                amsg.setRequiredHostUuid(spec.getDestHost().getUuid());
                if(spec.getRequiredPrimaryStorageUuidForDataVolume() != null && hasOtherNonLocalStoragePrimaryStorage){

                    PrimaryStorageVO requiredPrimaryStorageUuidForDataVolume = dbf.findByUuid(spec.getRequiredPrimaryStorageUuidForDataVolume(), PrimaryStorageVO.class);
                    // data volume ps set local
                    if(requiredPrimaryStorageUuidForDataVolume.getType().equals(LocalStorageConstants.LOCAL_STORAGE_TYPE)){
                        ErrorCode errorCode = operr("The cluster mounts multiple primary storage[%s(%s), other non-LocalStorage primary storage], primaryStorageUuidForDataVolume cannot be specified %s",
                                requiredPrimaryStorageUuidForDataVolume.getUuid(), requiredPrimaryStorageUuidForDataVolume.getType(),
                                LocalStorageConstants.LOCAL_STORAGE_TYPE);
                        trigger.fail(errorCode);
                        return;
                    }

                    amsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForDataVolume());
                } else if (spec.getRequiredPrimaryStorageUuidForDataVolume() != null && !hasOtherNonLocalStoragePrimaryStorage){
                    amsg.setRequiredPrimaryStorageUuid(spec.getRequiredPrimaryStorageUuidForDataVolume());
                } else if (hasOtherNonLocalStoragePrimaryStorage) {
                    amsg.setAllocationStrategy(dinv.getAllocatorStrategy());

                    List<String> localStorageUuids = getLocalStorageInCluster(spec.getDestHost().getClusterUuid());
                    for (String lsuuid : localStorageUuids) {
                        amsg.addExcludePrimaryStorageUuid(lsuuid);
                    }

                    amsg.addExcludeAllocatorStrategy(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);
                    logger.debug("there are non-local primary storage in the cluster, use it for data volumes");
                } else {
                    amsg.setAllocationStrategy(LocalStorageConstants.LOCAL_STORAGE_ALLOCATOR_STRATEGY);
                    amsg.setRequiredPrimaryStorageUuid(localStorageUuid);
                }

                amsg.setRequiredPrimaryStorageTypes(primaryStorageTypes);
                amsg.setDiskOfferingUuid(dinv.getUuid());
                bus.makeLocalServiceId(amsg, PrimaryStorageConstant.SERVICE_ID);
                msgs.add(amsg);
            }
        }

        bus.send(msgs, new CloudBusListCallBack(trigger) {
            @Override
            public void run(List<MessageReply> replies) {
                for (int i = 0; i < replies.size(); i++) {
                    MessageReply reply = replies.get(i);
                    if (!reply.isSuccess()) {
                        trigger.fail(reply.getError());
                        return;
                    }

                    AllocatePrimaryStorageReply ar = reply.castReply();
                    VolumeSpec vspec = new VolumeSpec();

                    if (i == 0) {
                        vspec.setSize(ar.getSize());
                        vspec.setPrimaryStorageInventory(ar.getPrimaryStorageInventory());
                        vspec.setRoot(true);
                    } else {
                        vspec.setSize(ar.getSize());
                        vspec.setPrimaryStorageInventory(ar.getPrimaryStorageInventory());
                        vspec.setDiskOfferingUuid(spec.getDataDiskOfferings().get(i - 1).getUuid());
                        vspec.setRoot(false);
                    }

                    spec.getVolumeSpecs().add(vspec);
                }

                trigger.next();
            }
        });
    }

    private List<String> getLocalStorageInCluster(String clusterUuid) {
        return SQL.New("select pri.uuid" +
                " from PrimaryStorageVO pri, PrimaryStorageClusterRefVO ref" +
                " where pri.uuid = ref.primaryStorageUuid" +
                " and ref.clusterUuid = :cuuid" +
                " and pri.type = :ptype", String.class)
                .param("cuuid", clusterUuid)
                .param("ptype", LocalStorageConstants.LOCAL_STORAGE_TYPE)
                .list();
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
        trigger.rollback();
    }
}
