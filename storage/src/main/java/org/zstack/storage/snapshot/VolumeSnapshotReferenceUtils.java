package org.zstack.storage.snapshot;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.core.parameters.P;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.ImageCacheVolumeRefVO;
import org.zstack.header.storage.primary.ImageCacheVolumeRefVO_;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.PrimaryStorageGlobalProperty;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VolumeSnapshotReferenceUtils {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotReferenceUtils.class);
    public static List<String> getAllReferenceVolumeUuids(VolumeSnapshotTree.SnapshotLeaf currentLeaf) {
        List<String> descendantUuids = currentLeaf.getDescendants().stream()
                .map(VolumeSnapshotInventory::getUuid)
                .collect(Collectors.toList());
        return Q.New(VolumeSnapshotReferenceVO.class).select(VolumeSnapshotReferenceVO_.referenceVolumeUuid)
                .in(VolumeSnapshotReferenceVO_.volumeSnapshotUuid, descendantUuids)
                .notNull(VolumeSnapshotReferenceVO_.referenceVolumeUuid)  // TODO REMOVE IT
                .listValues();
    }

    public static Map<String, List<String>> getDirectReferencedSnapshotUuidsGroupByTree(String volumeUuid) {
        List<Tuple> ts = SQL.New("select ref.volumeSnapshotUuid, snapshot.treeUuid" +
                        " from VolumeSnapshotReferenceVO ref, VolumeSnapshotVO snapshot" +
                        " where ref.volumeUuid = :volumeUuid" +
                        " and ref.volumeSnapshotUuid = snapshot.uuid" +
                        " and ref.referenceVolumeUuid is not null" + // TODO REMOVE IT
                        " group by ref.volumeSnapshotUuid", Tuple.class)
                .param("volumeUuid", volumeUuid)
                .list();

        return ts.stream().collect(Collectors.groupingBy(
                tuple -> tuple.get(1, String.class),
                Collectors.mapping(tuple -> tuple.get(0, String.class), Collectors.toList())
        ));
    }

    public static void handleChainVolumeSnapshotReferenceAfterFlatten(VolumeInventory volume) {
        VolumeSnapshotReferenceVO ref = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, volume.getUuid())
                .find();
        if (ref == null) {
            return;
        }

        String currentSnapshotTreeUuid = Q.New(VolumeSnapshotTreeVO.class).select(VolumeSnapshotTreeVO_.uuid)
                .eq(VolumeSnapshotTreeVO_.volumeUuid, volume.getUuid())
                .eq(VolumeSnapshotTreeVO_.current, true)
                .findValue();
        if (currentSnapshotTreeUuid == null) {
            deleteSnapshotRef(ref);
            return;
        }

        boolean volumeReferToOtherVols = !getVolumeReferenceSnapshotCurrently(volume.getUuid(), currentSnapshotTreeUuid).isEmpty();
        if (volumeReferToOtherVols) {
            return;
        }

        if (!isVolumeCurrentBaseSnapshotReference(volume, ref, currentSnapshotTreeUuid)) {
            return;
        }

        SQL.New(VolumeSnapshotVO.class).set(VolumeSnapshotVO_.state, VolumeSnapshotState.Disabled)
                .eq(VolumeSnapshotVO_.treeUuid, currentSnapshotTreeUuid)
                .update();
        logger.warn(String.format("disable current snapshot tree[volumeUuid: %s] because of volume flatten.", volume.getUuid()));

        deleteSnapshotRef(ref);
    }

    public static void handleStorageVolumeSnapshotReferenceAfterFlatten(VolumeInventory volume) {
        VolumeSnapshotReferenceVO ref = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, volume.getUuid())
                .eq(VolumeSnapshotReferenceVO_.referenceInstallUrl, volume.getInstallPath())
                .find();
        if (ref != null) {
            deleteSnapshotRef(ref);
        }
    }

    public static VolumeSnapshotReferenceVO buildSnapshotReferenceForNewVolumeIfNeed(VolumeInventory volume, String imageUuid) {
        VolumeSnapshotReferenceVO templateRef = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceUuid, imageUuid)
                .limit(1).find();
        if (templateRef == null) {
            return null;
        }

        DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
        // Deprecated
        if (!PrimaryStorageGlobalProperty.USE_SNAPSHOT_AS_INCREMENTAL_CACHE) {
            VolumeSnapshotReferenceVO volRef = templateRef.clone();
            volRef.setReferenceVolumeUuid(volume.getUuid());
            return dbf.persist(volRef);
        }

        VolumeSnapshotVO baseSnapshot = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, templateRef.getVolumeSnapshotUuid()).find();
        String baseImageUuid = Q.New(VolumeVO.class).select(VolumeVO_.rootImageUuid)
                .eq(VolumeVO_.uuid, baseSnapshot.getVolumeUuid())
                .findValue();
        return buildSnapshotReferenceForNewVolume(volume, baseSnapshot, baseImageUuid);
    }

    public static VolumeSnapshotReferenceVO buildSnapshotReferenceForNewVolume(VolumeInventory volume, VolumeSnapshotVO baseSnapshot, String baseImageUuid) {
        DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);

        VolumeSnapshotReferenceVO ref = new VolumeSnapshotReferenceVO();
        ref.setReferenceVolumeUuid(volume.getUuid());
        ref.setReferenceUuid(volume.getUuid());
        ref.setReferenceType(VolumeVO.class.getSimpleName());
        ref.setReferenceInstallUrl(volume.getInstallPath());
        ref.setVolumeSnapshotInstallUrl(baseSnapshot.getPrimaryStorageInstallPath());
        ref.setVolumeSnapshotUuid(baseSnapshot.getUuid());
        ref.setVolumeUuid(baseSnapshot.getVolumeUuid());

        if (VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString().equals(baseSnapshot.getType())) {
            logger.debug(String.format("create volume snapshot reference[volumeSnapshotUuid:%s, referVolumeUuid:%s]",
                    ref.getVolumeSnapshotUuid(), ref.getReferenceVolumeUuid()));
            return dbf.persist(ref);
        }

        VolumeSnapshotReferenceVO parentRef = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, baseSnapshot.getVolumeUuid())
                .find();

        if (parentRef != null && parentRef.getReferenceType().equals(VolumeSnapshotVO.class.getSimpleName())) {
            String parentSnapshotTreeUuid = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.treeUuid)
                    .eq(VolumeSnapshotVO_.uuid, parentRef.getReferenceUuid())
                    .findValue();
            if (!baseSnapshot.getTreeUuid().equals(parentSnapshotTreeUuid)) {
                parentRef = null;
            }
        }

        VolumeSnapshotReferenceTreeVO tree;
        if (parentRef == null) {
            tree = new VolumeSnapshotReferenceTreeVO();
            tree.setUuid(Platform.getUuid());
            tree.setRootImageUuid(baseImageUuid);
            dbf.persist(tree);
        } else {
            tree = Q.New(VolumeSnapshotReferenceTreeVO.class).eq(VolumeSnapshotReferenceTreeVO_.uuid, parentRef.getTreeUuid()).find();
            ref.setParentId(parentRef.getId());
        }

        ref.setTreeUuid(tree.getUuid());
        logger.debug(String.format("create volume snapshot reference[volumeSnapshotUuid:%s, referVolumeUuid:%s, treeUuid:%s]",
                ref.getVolumeSnapshotUuid(), ref.getReferenceVolumeUuid(), ref.getTreeUuid()));
        return dbf.persist(ref);
    }

    public static void rollbackSnapshotReferenceForNewVolume(String volumeUuid) {
        VolumeSnapshotReferenceVO ref = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, volumeUuid)
                .find();
        if (ref != null) {
            deleteSnapshotRef(ref);
        }
    }

    public static void updateReferenceAfterFirstSnapshot(VolumeSnapshotVO snapshot) {
        if (VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString().equals(snapshot.getType())) {
            return;
        }

        String refInstancePath = Q.New(VolumeSnapshotReferenceVO.class).select(VolumeSnapshotReferenceVO_.referenceInstallUrl)
                .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, snapshot.getVolumeUuid())
                .eq(VolumeSnapshotReferenceVO_.referenceUuid, snapshot.getVolumeUuid())
                .findValue();

        if (snapshot.getPrimaryStorageInstallPath().equals(refInstancePath)) {
            SQL.New(VolumeSnapshotReferenceVO.class)
                    .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, snapshot.getVolumeUuid())
                    .eq(VolumeSnapshotReferenceVO_.referenceUuid, snapshot.getVolumeUuid())
                    .set(VolumeSnapshotReferenceVO_.referenceUuid, snapshot.getUuid())
                    .set(VolumeSnapshotReferenceVO_.referenceType, VolumeSnapshotVO.class.getSimpleName())
                    .update();
        }
    }

    public static void cleanTemporaryImageReference(List<ImageInventory> images) {
        if (!PrimaryStorageGlobalProperty.USE_SNAPSHOT_AS_INCREMENTAL_CACHE || CollectionUtils.isEmpty(images)) {
            return;
        }

        images.stream().filter(it -> it.getMediaType().equals(ImageConstant.ImageMediaType.RootVolumeTemplate.toString())).findFirst().ifPresent(image -> {
            List<Long> cacheIds = Q.New(ImageCacheVO.class).select(ImageCacheVO_.id).eq(ImageCacheVO_.imageUuid, image.getUuid()).listValues();
            if (!cacheIds.isEmpty()) {
                SQL.New(ImageCacheVolumeRefVO.class).in(ImageCacheVolumeRefVO_.imageCacheId, cacheIds).delete();
                SQL.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, image.getUuid()).delete();
                SQL.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.referenceUuid, image.getUuid()).delete();
            }
        });
    }

    public static List<Long> filterStaleImageCache(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        return SQL.New("select c.id from ImageCacheVO c" +
                " where c.id in (:ids)" +
                " and c.imageUuid not in (select tree.rootImageUuid from VolumeSnapshotReferenceTreeVO tree)", Long.class)
                .param("ids", ids)
                .list();
    }

    public static void handleSnapshotDeletion(VolumeSnapshotVO snapshot) {
        if (snapshot == null || snapshot.getParentUuid() != null
                || VolumeSnapshotConstant.STORAGE_SNAPSHOT_TYPE.toString().equals(snapshot.getType())) {
            return;
        }

        VolumeSnapshotReferenceVO ref = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceUuid, snapshot.getUuid())
                .find();
        if (ref != null) {
            deleteSnapshotRef(ref);
        }
    }

    public static void handleVolumeDeletion(VolumeInventory volume) {
        if (volume == null) {
            return;
        }

        VolumeSnapshotReferenceVO ref = Q.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.referenceVolumeUuid, volume.getUuid())
                .find();
        if (ref != null) {
            deleteAndRedirectSnapshotRef(ref);
        }
    }

    private static void deleteAndRedirectSnapshotRef(VolumeSnapshotReferenceVO ref) {
        if (ref.getTreeUuid() == null) {
            deleteSnapshotRef(ref);
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                databaseFacade.getEntityManager().find(VolumeSnapshotReferenceTreeVO.class, ref.getTreeUuid(), LockModeType.PESSIMISTIC_WRITE);

                VolumeSnapshotReferenceVO finalRef =  databaseFacade.getEntityManager().find(VolumeSnapshotReferenceVO.class, ref.getId());
                List<Long> childrenRefIds = Q.New(VolumeSnapshotReferenceVO.class).select(VolumeSnapshotReferenceVO_.id)
                        .eq(VolumeSnapshotReferenceVO_.treeUuid, finalRef.getTreeUuid())
                        .eq(VolumeSnapshotReferenceVO_.parentId, finalRef.getId())
                        .listValues();

                if (!childrenRefIds.isEmpty()) {
                    sql(VolumeSnapshotReferenceVO.class).in(VolumeSnapshotReferenceVO_.id, childrenRefIds)
                            .set(VolumeSnapshotReferenceVO_.parentId, finalRef.getParentId())
                            .set(VolumeSnapshotReferenceVO_.volumeUuid, finalRef.getVolumeUuid())
                            .set(VolumeSnapshotReferenceVO_.volumeSnapshotUuid, finalRef.getVolumeSnapshotUuid())
                            .set(VolumeSnapshotReferenceVO_.volumeSnapshotInstallUrl, finalRef.getVolumeSnapshotInstallUrl())
                            .update();
                    logger.debug(String.format("redirect snapshot ref[ids:%s, volumeUuid: from %s to %s, parentId: to %s]",
                            childrenRefIds, finalRef.getReferenceVolumeUuid(), finalRef.getVolumeUuid(), finalRef.getParentId()));
                }

                deleteSnapshotRef(finalRef);
            }
        }.execute();
    }

    private static void deleteSnapshotRef(VolumeSnapshotReferenceVO ref) {
        SQL.New(VolumeSnapshotReferenceVO.class)
                .eq(VolumeSnapshotReferenceVO_.id, ref.getId())
                .hardDelete();
        logger.debug(String.format("remove volume snapshot reference[referenceVolumeUuid: %s].", ref.getReferenceVolumeUuid()));

        if (!Q.New(VolumeSnapshotReferenceVO.class).eq(VolumeSnapshotReferenceVO_.treeUuid, ref.getTreeUuid()).isExists()) {
            logger.debug(String.format("volume snapshot reference tree[uuid:%s] has no leaf, delete it", ref.getTreeUuid()));
            SQL.New(VolumeSnapshotReferenceTreeVO.class).eq(VolumeSnapshotReferenceTreeVO_.uuid, ref.getTreeUuid()).hardDelete();
        }
    }

    // for chain snapshot
    private static List<String> getVolumeReferenceSnapshotCurrently(String volumeUuid, String currentTreeUuid) {
        List<String> refSnapshotUuids = Q.New(VolumeSnapshotReferenceVO.class).select(VolumeSnapshotReferenceVO_.volumeSnapshotUuid)
                .eq(VolumeSnapshotReferenceVO_.volumeUuid, volumeUuid)
                .listValues();

        if (refSnapshotUuids.isEmpty()) {
            return Collections.emptyList();
        }

        return Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.uuid)
                .eq(VolumeSnapshotVO_.treeUuid, currentTreeUuid)
                .in(VolumeSnapshotVO_.uuid, refSnapshotUuids)
                .listValues();
    }

    // for chain snapshot
    private static boolean isVolumeCurrentBaseSnapshotReference(VolumeInventory volume, VolumeSnapshotReferenceVO ref, String currentTreeUuid) {
        if (ref.getReferenceType().equals(VolumeVO.class.getSimpleName()) ||
                volume.getInstallPath().equals(ref.getReferenceInstallUrl())) {
            return true;
        }

        List<String> currentSnapshotUuids = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.uuid)
                .eq(VolumeSnapshotVO_.treeUuid, currentTreeUuid)
                .listValues();
        if (currentSnapshotUuids.stream().anyMatch(it -> it.equals(ref.getReferenceUuid()))) {
            return true;
        }

        return false;
    }
}
