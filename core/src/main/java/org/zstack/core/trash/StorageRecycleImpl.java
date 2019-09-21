package org.zstack.core.trash;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.core.jsonlabel.JsonLabelVO_;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.core.trash.InstallPathRecycleInventory;
import org.zstack.header.core.trash.InstallPathRecycleVO;
import org.zstack.header.core.trash.InstallPathRecycleVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.snapshot.VolumeSnapshotAfterDeleteExtensionPoint;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.*;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.inerr;

/**
 * Created by mingjian.deng on 2019/9/19.
 */
public class StorageRecycleImpl implements StorageTrash, VolumeDeletionExtensionPoint, VolumeSnapshotAfterDeleteExtensionPoint, Component {
    private final static CLogger logger = Utils.getLogger(StorageRecycleImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;

    private String getResourceType(String resourceUuid) {
        ResourceVO vo = dbf.findByUuid(resourceUuid, ResourceVO.class);
        if (vo == null) {
            throw new OperationFailureException(inerr("cannot find ResourceVO for resourceUuid: %s, maybe it has been deleted", resourceUuid));
        }
        return vo.getResourceType();
    }

    private InstallPathRecycleInventory createRecycleFromVolume(TrashType type, boolean isFolder, final VolumeInventory vol) {
        InstallPathRecycleVO vo = new InstallPathRecycleVO();
        vo.setFolder(isFolder);
        vo.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(vol.getFormat()).toString());
        vo.setInstallPath(vol.getInstallPath());
        vo.setResourceType(VolumeVO.class.getSimpleName());
        vo.setResourceUuid(vol.getUuid());
        vo.setStorageUuid(vol.getPrimaryStorageUuid());
        vo.setStorageType(PrimaryStorageVO.class.getSimpleName());
        vo.setTrashType(type.toString());
        vo.setSize(vol.getSize());

        vo = dbf.persistAndRefresh(vo);
        return InstallPathRecycleInventory.valueOf(vo);
    }

    private InstallPathRecycleInventory createRecycleFromImage(TrashType type, boolean isFolder, final ImageInventory image) {
        InstallPathRecycleVO vo = new InstallPathRecycleVO();
        vo.setFolder(isFolder);
        vo.setInstallPath(image.getUrl());
        vo.setResourceType(ImageVO.class.getSimpleName());
        vo.setResourceUuid(image.getUuid());
        vo.setStorageUuid(image.getExportUrl());
        vo.setStorageType(BackupStorageVO.class.getSimpleName());
        vo.setTrashType(type.toString());
        vo.setSize(image.getSize());

        vo = dbf.persistAndRefresh(vo);
        return InstallPathRecycleInventory.valueOf(vo);
    }

    private InstallPathRecycleInventory createRecycleFromVolumeSnapshot(TrashType type, boolean isFolder, final VolumeSnapshotInventory snapshot) {
        InstallPathRecycleVO vo = new InstallPathRecycleVO();
        vo.setFolder(isFolder);
        vo.setHypervisorType(VolumeFormat.getMasterHypervisorTypeByVolumeFormat(snapshot.getFormat()).toString());
        vo.setInstallPath(snapshot.getPrimaryStorageInstallPath());
        vo.setResourceType(VolumeSnapshotVO.class.getSimpleName());
        vo.setResourceUuid(snapshot.getUuid());
        vo.setStorageUuid(snapshot.getPrimaryStorageUuid());
        vo.setStorageType(PrimaryStorageVO.class.getSimpleName());
        vo.setTrashType(type.toString());
        vo.setSize(snapshot.getSize());

        vo = dbf.persistAndRefresh(vo);
        return InstallPathRecycleInventory.valueOf(vo);
    }

    @Override
    public InstallPathRecycleInventory createTrash(TrashType type, boolean isFolder, Object o) {
        if (o instanceof VolumeInventory) {
            return createRecycleFromVolume(type, isFolder, (VolumeInventory)o);
        } else if (o instanceof ImageInventory) {
            return createRecycleFromImage(type, isFolder, (ImageInventory)o);
        } else if (o instanceof VolumeSnapshotInventory) {
            return createRecycleFromVolumeSnapshot(type, isFolder, (VolumeSnapshotInventory)o);
        } else {
            throw new OperationFailureException(inerr("non support resourceType to create trash"));
        }
    }

    @Override
    public List<InstallPathRecycleInventory> getTrashList(String storageUuid) {
        return getTrashList(storageUuid, CollectionDSL.list(TrashType.values()));
    }

    @Override
    public List<InstallPathRecycleInventory> getTrashList(String storageUuid, List<TrashType> types) {
        List<InstallPathRecycleInventory> invs = new ArrayList<>();
        for (TrashType type: types) {
            List<InstallPathRecycleVO> vos = Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.storageUuid, storageUuid).eq(InstallPathRecycleVO_.trashType, type.toString()).list();
            invs.addAll(InstallPathRecycleInventory.valueOf(vos));
        }
        return invs;
    }

    @Override
    public InstallPathRecycleInventory getTrash(Long trashId) {
        InstallPathRecycleVO vo = Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.trashId, trashId).find();
        if (vo == null) {
            return null;
        }
        return InstallPathRecycleInventory.valueOf(vo);
    }

    private boolean makeSureInstallPathNotUsedByVolume(String installPath) {
        return !Q.New(VolumeVO.class).eq(VolumeVO_.installPath, installPath).isExists() &&
                !Q.New(ImageCacheVO.class).eq(ImageCacheVO_.installUrl, installPath).isExists() &&
                !Q.New(VolumeSnapshotVO.class).like(VolumeSnapshotVO_.primaryStorageInstallPath, installPath + "@%").isExists();
    }

    private boolean makeSureInstallPathNotUsedByImage(String installPath) {
        return !Q.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.installPath, installPath).isExists();
    }

    private boolean makeSureInstallPathNotUsedBySnapshot(String installPath) {
        return !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.primaryStorageInstallPath, installPath).isExists() &&
                !Q.New(ImageCacheVO.class).eq(ImageCacheVO_.installUrl, installPath).isExists();
    }

    @Override
    public boolean makeSureInstallPathNotUsed(InstallPathRecycleInventory inv) {
        if (inv.getResourceType().equals(VolumeVO.class.getSimpleName())) {
            return makeSureInstallPathNotUsedByVolume(inv.getInstallPath());
        } else if (inv.getResourceType().equals(ImageVO.class.getSimpleName())) {
            return makeSureInstallPathNotUsedByImage(inv.getInstallPath());
        } else if (inv.getResourceType().equals(VolumeSnapshotVO.class.getSimpleName())) {
            return makeSureInstallPathNotUsedBySnapshot(inv.getInstallPath());
        }
        return true;
    }

    @Override
    public Long getTrashId(String storageUuid, String installPath) {
        DebugUtils.Assert(installPath != null, "installPath is not allowed null here");
        List<InstallPathRecycleVO> vos = Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.storageUuid, storageUuid).like(InstallPathRecycleVO_.installPath, String.format("%%%s%%", installPath)).list();
        if (!vos.isEmpty()) {
            for (InstallPathRecycleVO vo: vos) {
                if (vo.getInstallPath().equals(installPath)) {
                    return vo.getTrashId();
                }
            }
        }
        return null;
    }

    @Override
    public void removeFromDb(Long trashId) {
        DebugUtils.Assert(trashId != null, "trashId is not allowed null here");
        UpdateQuery.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.trashId, trashId).delete();
    }

    private void deleteTrashForVolume(String resourceUuid, String primaryStorageUuid, Completion completion) {
        List<InstallPathRecycleVO> vos = Q.New(InstallPathRecycleVO.class).eq(InstallPathRecycleVO_.storageUuid, primaryStorageUuid).list();
        List<Long> trashIds = new ArrayList<>();
        for (InstallPathRecycleVO vo: vos) {
            if (vo.getResourceUuid().equals(resourceUuid)) {
                trashIds.add(vo.getTrashId());
            }
        }

        deleteTrashForVolume(trashIds.iterator(), primaryStorageUuid, completion);
    }

    private void deleteTrashForVolume(final Iterator<Long> trashIds, String primaryStorageUuid, Completion completion) {
        if (!trashIds.hasNext()) {
            completion.success();
            return;
        }
        Long trashId = trashIds.next();

        CleanUpTrashOnPrimaryStroageMsg pmsg = new CleanUpTrashOnPrimaryStroageMsg();
        pmsg.setPrimaryStorageUuid(primaryStorageUuid);
        pmsg.setTrashId(trashId);
        bus.makeTargetServiceIdByResourceUuid(pmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(pmsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("clean up trash [%s] on primary storage [%s] failed, because: %s",
                            pmsg.getTrashId(), primaryStorageUuid, reply.getError().getDetails()));
                }
                deleteTrashForVolume(trashIds, primaryStorageUuid, completion);
            }
        });
    }

    @Override
    public void volumeSnapshotAfterDeleteExtensionPoint(VolumeSnapshotInventory snapshot, Completion completion) {
        deleteTrashForVolume(snapshot.getUuid(), snapshot.getPrimaryStorageUuid(), completion);
    }

    @Override
    public void volumeSnapshotAfterFailedDeleteExtensionPoint(VolumeSnapshotInventory snapshot) {

    }

    @Override
    public void preDeleteVolume(VolumeInventory volume) {

    }

    @Override
    public void beforeDeleteVolume(VolumeInventory volume) {

    }

    @Override
    public void afterDeleteVolume(VolumeInventory volume, Completion completion) {
        deleteTrashForVolume(volume.getUuid(), volume.getPrimaryStorageUuid(), completion);
    }

    @Override
    public void failedToDeleteVolume(VolumeInventory volume, ErrorCode errorCode) {

    }

    private void transfer(List<JsonLabelVO> trashs, String type) {
        List<String> recycles = new ArrayList<>();
        for (JsonLabelVO trash: trashs) {
            InstallPathRecycleVO vo = JSONObjectUtil.toObject(trash.getLabelValue(), InstallPathRecycleVO.class);
            vo = dbf.persistAndRefresh(vo);
            recycles.add(String.valueOf(vo.getTrashId()));
        }
        for (JsonLabelVO trash: trashs) {
            dbf.remove(trash);
        }
        if (!recycles.isEmpty()) {
            logger.debug(String.format("transfer %s trash to recycles from %s: %s", recycles.size(), type, recycles));
        }
    }

    private void transferTrashDataFromOldVersion() {
        for (TrashType type: TrashType.values()) {
            List<JsonLabelVO> vos = Q.New(JsonLabelVO.class).like(JsonLabelVO_.labelKey, type.toString() + "-%").list();
            transfer(vos, type.toString());
        }
    }

    @Override
    public boolean start() {
        transferTrashDataFromOldVersion();
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            return stop();
        } else {
            // we insert test data in ut, so we start a timer for test case
            thdf.submitPeriodicTask(new PeriodicTask(){
                @Override
                public TimeUnit getTimeUnit() {
                    return TimeUnit.MILLISECONDS;
                }

                @Override
                public long getInterval() {
                    return 100;
                }

                @Override
                public String getName() {
                    return "transferTrashDataFromOldVersion";
                }

                @Override
                public void run() {
                    transferTrashDataFromOldVersion();
                }
            });
            return true;
        }
    }

    @Override
    public boolean stop() {
        return true;
    }
}
