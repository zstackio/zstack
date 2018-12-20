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
import org.zstack.header.storage.backup.StorageTrashSpec;
import org.zstack.header.vo.ResourceVO;
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

        return new JsonLabel().create(makeTrashType(type), spec, spec.getStorageUuid());
    }

    private String makeTrashType(TrashType type) {
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
                spec.setId(l.getId());
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
        spec.setId(trashId);
        spec.setCreateDate(lable.getCreateDate());
        return spec;
    }

    @Override
    public void remove(Long trashId) {
        DebugUtils.Assert(trashId != null, "trashId is not allowed null here");
        UpdateQuery.New(JsonLabelVO.class).eq(JsonLabelVO_.id, trashId).delete();
    }
}
