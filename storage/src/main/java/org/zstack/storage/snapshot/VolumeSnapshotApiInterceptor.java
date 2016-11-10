package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO;
import org.zstack.header.storage.backup.BackupStorageZoneRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.*;

import javax.persistence.Tuple;
import java.util.List;

/**
 */
public class VolumeSnapshotApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof VolumeSnapshotMessage) {
            VolumeSnapshotMessage vmsg = (VolumeSnapshotMessage) msg;
            SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
            q.select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid);
            q.add(VolumeSnapshotVO_.uuid, SimpleQuery.Op.EQ, vmsg.getSnapshotUuid());
            Tuple t = q.findTuple();
            String volumeUuid = t.get(0, String.class);
            String treeUuid = t.get(1, String.class);
            vmsg.setVolumeUuid(volumeUuid);
            vmsg.setTreeUuid(treeUuid);
            String resourceUuid = volumeUuid != null ? volumeUuid : treeUuid;
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
        } else if (msg instanceof APICreateVolumeSnapshotSchedulerMsg) {
            validate((APICreateVolumeSnapshotSchedulerMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotMsg) {
            validate((APICreateVolumeSnapshotMsg) msg);
        } else if (msg instanceof APIGetVolumeSnapshotTreeMsg) {
            validate((APIGetVolumeSnapshotTreeMsg) msg);
        } else if (msg instanceof APIBackupVolumeSnapshotMsg) {
            validate((APIBackupVolumeSnapshotMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIBackupVolumeSnapshotMsg msg) {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.primaryStorageUuid);
        q.add(VolumeSnapshotVO_.uuid, Op.EQ, msg.getUuid());
        String priUuid = q.findValue();
        if (priUuid == null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("volume snapshot[uuid:%s] is not on primary storage, cannot be backed up", msg.getUuid())
            ));
        }

        if (msg.getBackupStorageUuid() != null) {
            SimpleQuery<VolumeSnapshotBackupStorageRefVO> rq = dbf.createQuery(VolumeSnapshotBackupStorageRefVO.class);
            rq.add(VolumeSnapshotBackupStorageRefVO_.volumeSnapshotUuid, Op.EQ, msg.getUuid());
            rq.add(VolumeSnapshotBackupStorageRefVO_.backupStorageUuid, Op.EQ, msg.getBackupStorageUuid());
            if (rq.isExists()) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                        String.format("volume snapshot[uuid:%s] is already on backup storage[uuid: %s]", msg.getUuid(), msg.getBackupStorageUuid())
                ));
            }

            SimpleQuery<PrimaryStorageVO> pq = dbf.createQuery(PrimaryStorageVO.class);
            pq.select(PrimaryStorageVO_.zoneUuid);
            pq.add(PrimaryStorageVO_.uuid, Op.EQ, priUuid);
            String zoneUuid = pq.findValue();

            SimpleQuery<BackupStorageZoneRefVO> brq = dbf.createQuery(BackupStorageZoneRefVO.class);
            brq.add(BackupStorageZoneRefVO_.zoneUuid, Op.EQ, zoneUuid);
            brq.add(BackupStorageZoneRefVO_.backupStorageUuid, Op.EQ, msg.getBackupStorageUuid());
            if (!brq.isExists()) {
                throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                        String.format(String.format("volume snapshot[uuid:%s] is on primary storage[uuid:%s] which is in zone[uuid:%s] that backup storage[uuid:%s] is not attached to",
                                msg.getUuid(), priUuid, zoneUuid, msg.getBackupStorageUuid()))
                ));
            }
        }
    }

    private void validate(APIGetVolumeSnapshotTreeMsg msg) {
        if (msg.getTreeUuid() == null && msg.getVolumeUuid() == null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("either volumeUuid or treeUuid must be set")
            ));
        }
    }

    private void validate(APICreateVolumeSnapshotMsg msg) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.status);
        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
        VolumeStatus status = q.findValue();
        if (status != VolumeStatus.Ready) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("volume[uuid:%s] is not in status Ready, current is %s, can't create snapshot", msg.getVolumeUuid(), status)
            ));
        }
    }

    private void validate(APICreateVolumeSnapshotSchedulerMsg msg) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.status);
        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
        VolumeStatus status = q.findValue();
        if (status != VolumeStatus.Ready) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("volume[uuid:%s] is not in status Ready, current is %s, can't create snapshot", msg.getVolumeUuid(), status)
            ));
        }
    }

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
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("volume snapshot[uuid:%s] is in state %s, cannot revert volume to it", msg.getUuid(), state)
            ));
        }

        String volUuid = t.get(1, String.class);
        if (volUuid == null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.OPERATION_ERROR,
                    String.format("original volume for snapshot[uuid:%s] has been deleted, cannot revert volume to it", msg.getUuid())
            ));
        }
    }

    private void validate(APIDeleteVolumeSnapshotMsg msg) {
        if (!dbf.isExist(msg.getUuid(), VolumeSnapshotVO.class)) {
            APIDeleteVolumeSnapshotEvent evt = new APIDeleteVolumeSnapshotEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }
}
