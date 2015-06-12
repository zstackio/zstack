package org.zstack.storage.primary.iscsi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.CreateIscsiTargetCmd;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.CreateIscsiTargetRsp;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.DeleteIscsiTargetCmd;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageCommands.DeleteIscsiTargetRsp;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 6/9/2015.
 */
public class IscsiFileSystemPrimaryStorageTargetExtension implements VmInstanceStopExtensionPoint,
        VmAttachVolumeExtensionPoint, VmDetachVolumeExtensionPoint, VmInstanceStartExtensionPoint {
    private CLogger logger = Utils.getLogger(IscsiFileSystemPrimaryStorageTargetExtension.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public String preStopVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        deleteTarget(inv.getAllVolumes());
    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    @Override
    public String preStartVm(VmInstanceInventory inv) {
        createTarget(inv.getAllVolumes());
        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {

    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {

    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {

    }

    private class VolumeIscsiPrimaryStorageStruct {
        VolumeInventory volume;
        IscsiFileSystemBackendPrimaryStorageVO primaryStorage;
    }

    @Transactional(readOnly = true)
    private List<VolumeIscsiPrimaryStorageStruct> buildStructs(List<VolumeInventory> volumes) {
        final List<String> vuuids = CollectionUtils.transformToList(volumes, new Function<String, VolumeInventory>() {
            @Override
            public String call(VolumeInventory arg) {
                return VolumeStatus.NotInstantiated.toString().equals(arg.getStatus()) ? null : arg.getUuid();
            }
        });

        List<VolumeIscsiPrimaryStorageStruct> res = new ArrayList<VolumeIscsiPrimaryStorageStruct>();

        if (vuuids.isEmpty()) {
            return res;
        }

        String sql = "select i from IscsiFileSystemBackendPrimaryStorageVO i, VolumeVO vol where vol.primaryStorageUuid = i.uuid and vol.uuid in (:volUuids) group by i.uuid";
        TypedQuery<IscsiFileSystemBackendPrimaryStorageVO> q = dbf.getEntityManager().createQuery(sql, IscsiFileSystemBackendPrimaryStorageVO.class);
        q.setParameter("volUuids", vuuids);
        List<IscsiFileSystemBackendPrimaryStorageVO> vos = q.getResultList();

        for (IscsiFileSystemBackendPrimaryStorageVO vo : vos) {
            for (VolumeInventory vol : volumes) {
                if (vo.getUuid().equals(vol.getPrimaryStorageUuid())) {
                    VolumeIscsiPrimaryStorageStruct s = new VolumeIscsiPrimaryStorageStruct();
                    s.primaryStorage = vo;
                    s.volume = vol;
                    res.add(s);
                }
            }
        }

        return res;
    }

    private void createTarget(List<VolumeInventory> volumes) {
        List<VolumeIscsiPrimaryStorageStruct> structs = buildStructs(volumes);
        if (structs.isEmpty()) {
            return;
        }

        List<IscsiBtrfsPrimaryStorageAsyncCallMsg> msgs = new ArrayList<IscsiBtrfsPrimaryStorageAsyncCallMsg>();
        for (VolumeIscsiPrimaryStorageStruct s : structs) {
            IscsiVolumePath path = new IscsiVolumePath(s.volume.getInstallPath());
            path.disassemble();

            CreateIscsiTargetCmd cmd = new CreateIscsiTargetCmd();
            cmd.setInstallPath(path.getInstallPath());
            cmd.setVolumeUuid(s.volume.getUuid());
            cmd.setChapPassword(s.primaryStorage.getChapPassword());
            cmd.setChapUsername(s.primaryStorage.getChapUsername());

            IscsiBtrfsPrimaryStorageAsyncCallMsg msg = new IscsiBtrfsPrimaryStorageAsyncCallMsg();
            msg.setCommand(cmd);
            msg.setPath(IscsiBtrfsPrimaryStorageConstants.CREATE_TARGET_PATH);
            msg.setPrimaryStorageUuid(s.primaryStorage.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, s.primaryStorage.getUuid());
            msgs.add(msg);
        }

        List<MessageReply> replies = bus.call(msgs);
        for (MessageReply r : replies) {
            if (!r.isSuccess()) {
                // no need to rollback created targets, the agent is designed as skipping existing targets
                // so it's no harm for next time creating targets where targets are already there
                throw new OperationFailureException(r.getError());
            } else {
                IscsiBtrfsPrimaryStorageAsyncCallReply ir = r.castReply();
                CreateIscsiTargetRsp rsp = ir.toResponse(CreateIscsiTargetRsp.class);
                if (!ir.isSuccess()) {
                    throw new OperationFailureException(errf.stringToOperationError(rsp.getError()));
                }
            }
        }
    }

    private void deleteTarget(List<VolumeInventory> volumes) {
        List<VolumeIscsiPrimaryStorageStruct> structs = buildStructs(volumes);
        if (structs.isEmpty()) {
            return;
        }

        List<IscsiBtrfsPrimaryStorageAsyncCallMsg> msgs = new ArrayList<IscsiBtrfsPrimaryStorageAsyncCallMsg>();
        for (VolumeIscsiPrimaryStorageStruct s : structs) {
            IscsiVolumePath path = new IscsiVolumePath(s.volume.getInstallPath());
            path.disassemble();

            DeleteIscsiTargetCmd cmd = new DeleteIscsiTargetCmd();
            cmd.setTarget(path.getTarget());
            cmd.setUuid(s.volume.getUuid());

            IscsiBtrfsPrimaryStorageAsyncCallMsg msg = new IscsiBtrfsPrimaryStorageAsyncCallMsg();
            msg.setCommand(cmd);
            msg.setPath(IscsiBtrfsPrimaryStorageConstants.DELETE_TARGET_PATH);
            msg.setPrimaryStorageUuid(s.primaryStorage.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, s.primaryStorage.getUuid());
            msgs.add(msg);
        }

        List<MessageReply> replies = bus.call(msgs);
        for (MessageReply r : replies) {
            if (!r.isSuccess()) {
                VolumeIscsiPrimaryStorageStruct s = structs.get(replies.indexOf(r));
                logger.warn(String.format("failed to delete iscsi target for volume[uuid:%s, path:%s] on btrfs primary storage[uuid:%s], %s. It's fine, it won't harm your system",
                        s.volume.getUuid(), s.volume.getInstallPath(), s.primaryStorage.getUuid(), r.getError()));
            } else {
                VolumeIscsiPrimaryStorageStruct s = structs.get(replies.indexOf(r));
                IscsiBtrfsPrimaryStorageAsyncCallReply ir = r.castReply();
                DeleteIscsiTargetRsp rsp = ir.toResponse(DeleteIscsiTargetRsp.class);
                if (!rsp.isSuccess()) {
                    logger.warn(String.format("failed to delete iscsi target for volume[uuid:%s, path:%s] on btrfs primary storage[uuid:%s], %s. It's fine, it won't harm your system",
                            s.volume.getUuid(), s.volume.getInstallPath(), s.primaryStorage.getUuid(), rsp.getError()));
                }
            }
        }
    }

    @Override
    public void preAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        createTarget(list(volume));
    }

    @Override
    public void beforeAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void afterAttachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void failedToAttachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {

    }

    @Override
    public void preDetachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void beforeDetachVolume(VmInstanceInventory vm, VolumeInventory volume) {

    }

    @Override
    public void afterDetachVolume(VmInstanceInventory vm, VolumeInventory volume) {
        deleteTarget(list(volume));
    }

    @Override
    public void failedToDetachVolume(VmInstanceInventory vm, VolumeInventory volume, ErrorCode errorCode) {

    }
}
