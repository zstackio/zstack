package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.APIRevertVmFromSnapshotGroupMsg;
import org.zstack.header.storage.snapshot.group.MemorySnapshotValidatorExtensionPoint;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.storage.snapshot.VolumeSnapshotMessageRouter.getResourceIdToRouteMsg;

import javax.persistence.Tuple;
import java.util.Arrays;
import java.util.List;


/**
 */
public class VolumeSnapshotApiInterceptor implements ApiMessageInterceptor {
    private final static CLogger logger = Utils.getLogger(VolumeSnapshotApiInterceptor.class);
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    protected PluginRegistry pluginRgty;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VolumeSnapshotMessage) {
            VolumeSnapshotMessage vmsg = (VolumeSnapshotMessage) msg;
            VolumeSnapshotVO snapshot = dbf.findByUuid(vmsg.getSnapshotUuid(), VolumeSnapshotVO.class);
            vmsg.setVolumeUuid(snapshot.getVolumeUuid());
            vmsg.setTreeUuid(snapshot.getTreeUuid());
            String resourceUuid = getResourceIdToRouteMsg(snapshot);
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIDeleteVolumeSnapshotMsg) {
            validate((APIDeleteVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIRevertVolumeFromSnapshotMsg) {
            validate((APIRevertVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof APIDeleteVolumeSnapshotFromBackupStorageMsg) {
            validate((APIDeleteVolumeSnapshotFromBackupStorageMsg) msg);
//        } else if (msg instanceof APIGetVolumeSnapshotTreeMsg) {
//            validate((APIGetVolumeSnapshotTreeMsg) msg);
//        } else if (msg instanceof APIBackupVolumeSnapshotMsg) {
//            validate((APIBackupVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIBatchDeleteVolumeSnapshotMsg) {
            validate((APIBatchDeleteVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIRevertVmFromSnapshotGroupMsg) {
            validate((APIRevertVmFromSnapshotGroupMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private boolean isWithMemoryForSnapshotGroup(VolumeSnapshotGroupVO groupVO) {
        return groupVO.getVolumeSnapshotRefs().stream().anyMatch(ref -> ref.getVolumeType().equals(VolumeType.Memory.toString()));
    }

    private void validate(APIRevertVmFromSnapshotGroupMsg msg) {
        VolumeSnapshotGroupVO group = dbf.findByUuid(msg.getUuid(), VolumeSnapshotGroupVO.class);

        for (MemorySnapshotValidatorExtensionPoint ext : pluginRgty.getExtensionList(MemorySnapshotValidatorExtensionPoint.class)) {
            if (!isWithMemoryForSnapshotGroup(group)) {
                break;
            }

            ErrorCode errorCode = ext.checkVmWhereMemorySnapshotExistExternalDevices(group.getVmInstanceUuid());
            if (errorCode != null) {
                throw new ApiMessageInterceptionException(errorCode);
            }
        }

        if (isWithMemoryForSnapshotGroup(group)
                && Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, group.getVmInstanceUuid())
                .in(VmInstanceVO_.state, Arrays.asList(VmInstanceState.Running, VmInstanceState.Paused))
                .isExists()) {
            throw new ApiMessageInterceptionException(argerr("Can not take memory snapshot, expected vm states are [%s, %s]",
                    VmInstanceState.Running.toString(), VmInstanceState.Paused.toString()));
        }
    }
/*
    private void validate(APIBackupVolumeSnapshotMsg msg) {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.primaryStorageUuid);
        q.add(VolumeSnapshotVO_.uuid, Op.EQ, msg.getUuid());
        String priUuid = q.findValue();
        if (priUuid == null) {
            throw new ApiMessageInterceptionException(operr("volume snapshot[uuid:%s] is not on primary storage, cannot be backed up", msg.getUuid()));
        }

        if (msg.getBackupStorageUuid() != null) {
            SimpleQuery<VolumeSnapshotBackupStorageRefVO> rq = dbf.createQuery(VolumeSnapshotBackupStorageRefVO.class);
            rq.add(VolumeSnapshotBackupStorageRefVO_.volumeSnapshotUuid, Op.EQ, msg.getUuid());
            rq.add(VolumeSnapshotBackupStorageRefVO_.backupStorageUuid, Op.EQ, msg.getBackupStorageUuid());
            if (rq.isExists()) {
                throw new ApiMessageInterceptionException(operr("volume snapshot[uuid:%s] is already on backup storage[uuid: %s]", msg.getUuid(), msg.getBackupStorageUuid()));
            }

            SimpleQuery<PrimaryStorageVO> pq = dbf.createQuery(PrimaryStorageVO.class);
            pq.select(PrimaryStorageVO_.zoneUuid);
            pq.add(PrimaryStorageVO_.uuid, Op.EQ, priUuid);
            String zoneUuid = pq.findValue();

            SimpleQuery<BackupStorageZoneRefVO> brq = dbf.createQuery(BackupStorageZoneRefVO.class);
            brq.add(BackupStorageZoneRefVO_.zoneUuid, Op.EQ, zoneUuid);
            brq.add(BackupStorageZoneRefVO_.backupStorageUuid, Op.EQ, msg.getBackupStorageUuid());
            if (!brq.isExists()) {
                throw new ApiMessageInterceptionException(operr("volume snapshot[uuid:%s] is on primary storage[uuid:%s] which is in zone[uuid:%s] that backup storage[uuid:%s] is not attached to",
                                msg.getUuid(), priUuid, zoneUuid, msg.getBackupStorageUuid()));
            }
        }
    }

    private void validate(APIGetVolumeSnapshotTreeMsg msg) {
        if (msg.getTreeUuid() == null && msg.getVolumeUuid() == null) {
            throw new ApiMessageInterceptionException(argerr("either volumeUuid or treeUuid must be set"));
        }
    }
*/

    private void validate(APIDeleteVolumeSnapshotFromBackupStorageMsg msg) {
        SimpleQuery<VolumeSnapshotBackupStorageRefVO> q = dbf.createQuery(VolumeSnapshotBackupStorageRefVO.class);
        q.select(VolumeSnapshotBackupStorageRefVO_.backupStorageUuid);
        q.add(VolumeSnapshotBackupStorageRefVO_.volumeSnapshotUuid, Op.EQ, msg.getSnapshotUuid());
        if (!msg.getBackupStorageUuids().isEmpty()) {
            q.add(VolumeSnapshotBackupStorageRefVO_.backupStorageUuid, Op.IN, msg.getBackupStorageUuids());
        }
        List<String> bsUuids = q.listValue();
        if (bsUuids.isEmpty()) {
            APIDeleteVolumeSnapshotFromBackupStorageEvent evt = new APIDeleteVolumeSnapshotFromBackupStorageEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
        msg.setBackupStorageUuids(bsUuids);
    }

    private void validate(APIRevertVolumeFromSnapshotMsg msg) {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.state, VolumeSnapshotVO_.volumeUuid);
        q.add(VolumeSnapshotVO_.uuid, Op.EQ, msg.getUuid());
        Tuple t = q.findTuple();
        VolumeSnapshotState state = t.get(0, VolumeSnapshotState.class);
        if (state != VolumeSnapshotState.Enabled) {
            throw new ApiMessageInterceptionException(operr("volume snapshot[uuid:%s] is in state %s, cannot revert volume to it", msg.getUuid(), state));
        }

        String volUuid = t.get(1, String.class);
        if (volUuid == null) {
            throw new ApiMessageInterceptionException(operr("original volume for snapshot[uuid:%s] has been deleted, cannot revert volume to it", msg.getUuid()));
        }
    }

    private void validate(APIDeleteVolumeSnapshotMsg msg) {
        if (!dbf.isExist(msg.getUuid(), VolumeSnapshotVO.class)) {
            APIDeleteVolumeSnapshotEvent evt = new APIDeleteVolumeSnapshotEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }

    private void validate(APIBatchDeleteVolumeSnapshotMsg msg) {
        List<VolumeSnapshotVO> snapshotVOS = Q.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, msg.getUuids()).list();
        for (VolumeSnapshotVO snapshotVO : snapshotVOS) {
            if (msg.getVolumeUuid() == null) {
                msg.setVolumeUuid(snapshotVO.getVolumeUuid());
            } else if (!snapshotVO.getVolumeUuid().equals(msg.getVolumeUuid())) {
                throw new ApiMessageInterceptionException(operr("not support delete snapshots on different volumes[uuid: %s, %s]", msg.getVolumeUuid(), snapshotVO.getVolumeUuid()));
            }
        }
        if (msg.getVolumeUuid() == null) {
            throw new ApiMessageInterceptionException(operr("can not find volume uuid for snapshosts[uuid: %s]", msg.getUuids()));
        }
    }
}
