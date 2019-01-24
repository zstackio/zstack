package org.zstack.core.trash;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelInventory;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.core.jsonlabel.JsonLabelVO_;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.backup.StorageTrashSpec;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.CollectionDSL;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.inerr;

/**
 * Created by mingjian.deng on 2018/12/13.
 */
public class StorageTrashImpl implements StorageTrash {
    private final static CLogger logger = Utils.getLogger(StorageTrashImpl.class);

    @Autowired
    private DatabaseFacade dbf;

    private String getResourceType(String resourceUuid) {
        ResourceVO vo = dbf.findByUuid(resourceUuid, ResourceVO.class);
        if (vo == null) {
            throw new OperationFailureException(inerr("cannot find ResourceVO for resourceUuid: %s, maybe it has been deleted", resourceUuid));
        }
        return vo.getResourceType();
    }

    @Override
    public JsonLabelInventory createTrash(TrashType type, StorageTrashSpec spec) {
        if (spec.getStorageUuid() == null || spec.getResourceUuid() == null) {
            throw new OperationFailureException(inerr("both resourceUuid and storageUuid must be set for createTrash"));
        }
        if (spec.getResourceType() == null) {
            spec.setResourceType(getResourceType(spec.getResourceUuid()));
        }

        if (spec.getStorageType() == null) {
            spec.setStorageType(getResourceType(spec.getStorageUuid()));
        }

        return new JsonLabel().create(makeTrashKey(type), spec, spec.getStorageUuid());
    }

    private String makeTrashKey(TrashType type) {
        return String.format("%s-%s", type.toString(), Platform.getUuid());
    }

    @Override
    public Map<String, StorageTrashSpec> getTrashList(String storageUuid) {
        return getTrashList(storageUuid, CollectionDSL.list(TrashType.values()));
    }

    @Override
    public Map<String, StorageTrashSpec> getTrashList(String storageUuid, List<TrashType> types) {
        Map<String, StorageTrashSpec> specs = new HashMap<>();
        for (TrashType type: types) {
            List<JsonLabelVO> labels = Q.New(JsonLabelVO.class).eq(JsonLabelVO_.resourceUuid, storageUuid).like(JsonLabelVO_.labelKey, type.toString() + "-%").list();
            labels.forEach(l -> {
                StorageTrashSpec spec = JSONObjectUtil.toObject(l.getLabelValue(), StorageTrashSpec.class);
                spec.setTrashId(l.getId());
                spec.setCreateDate(l.getCreateDate());
                specs.put(l.getLabelKey(), spec);
            });
        }
        return specs;
    }

    @Override
    public void remove(String trashKey, String storageUuid) {
        UpdateQuery.New(JsonLabelVO.class).eq(JsonLabelVO_.labelKey, trashKey).eq(JsonLabelVO_.resourceUuid, storageUuid).delete();
    }

    @Override
    public StorageTrashSpec getTrash(String storageUuid, Long trashId) {
        JsonLabelVO lable = Q.New(JsonLabelVO.class).eq(JsonLabelVO_.resourceUuid, storageUuid).eq(JsonLabelVO_.id, trashId).find();
        if (lable == null) {
            return null;
        }
        StorageTrashSpec spec = JSONObjectUtil.toObject(lable.getLabelValue(), StorageTrashSpec.class);
        spec.setTrashId(trashId);
        spec.setCreateDate(lable.getCreateDate());
        return spec;
    }

    @Override
    public void remove(Long trashId) {
        DebugUtils.Assert(trashId != null, "trashId is not allowed null here");
        UpdateQuery.New(JsonLabelVO.class).eq(JsonLabelVO_.id, trashId).delete();
    }

    @Override
    public Long getTrashId(String storageUuid, String installPath) {
        DebugUtils.Assert(installPath != null, "installPath is not allowed null here");
        List<JsonLabelVO> lables = Q.New(JsonLabelVO.class).eq(JsonLabelVO_.resourceUuid, storageUuid).like(JsonLabelVO_.labelValue, String.format("%%%s%%", installPath)).list();
        if (!lables.isEmpty()) {
            for (JsonLabelVO lable: lables) {
                for (TrashType type: TrashType.values()) {
                    if (!lable.getLabelKey().startsWith(type.name())) {
                        // if lable key not starts with type, it may not be storage trash
                        continue;
                    }
                }
                StorageTrashSpec spec = JSONObjectUtil.toObject(lable.getLabelValue(), StorageTrashSpec.class);
                if (spec.getInstallPath().equals(installPath)) {
                    return lable.getId();
                }
            }
        }
        return null;
    }

    private boolean makeSureInstallPathNotUsedByVolume(String installPath) {
        return !Q.New(VolumeVO.class).eq(VolumeVO_.installPath, installPath).isExists() &&
                !Q.New(ImageCacheVO.class).eq(ImageCacheVO_.installUrl, installPath).isExists();
    }

    private boolean makeSureInstallPathNotUsedByImage(String installPath) {
        return !Q.New(ImageBackupStorageRefVO.class).eq(ImageBackupStorageRefVO_.installPath, installPath).isExists();
    }

    private boolean makeSureInstallPathNotUsedBySnapshot(String installPath) {
        return !Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.primaryStorageInstallPath, installPath).isExists() &&
                !Q.New(ImageCacheVO.class).eq(ImageCacheVO_.installUrl, installPath).isExists();
    }

    @Override
    public boolean makeSureInstallPathNotUsed(StorageTrashSpec spec) {
        if (spec.getResourceType().equals(VolumeVO.class.getSimpleName())) {
            return makeSureInstallPathNotUsedByVolume(spec.getInstallPath());
        } else if (spec.getResourceType().equals(ImageVO.class.getSimpleName())) {
            return makeSureInstallPathNotUsedByImage(spec.getInstallPath());
        } else if (spec.getResourceType().equals(VolumeSnapshotVO.class.getSimpleName())) {
            return makeSureInstallPathNotUsedBySnapshot(spec.getInstallPath());
        }
        return true;
    }
}
