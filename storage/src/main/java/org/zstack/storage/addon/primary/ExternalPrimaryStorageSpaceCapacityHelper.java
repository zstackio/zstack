package org.zstack.storage.addon.primary;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DeadlockAutoRestart;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.addon.StorageCapacity;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageSpaceVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageSpaceVO_;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.PrimaryStorageCapacityChecker;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.stopwatch.StopWatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ExternalPrimaryStorageSpaceCapacityHelper extends ExternalPrimaryStorageSpaceHelper {
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected ExternalPrimaryStorageFactory extFactory;
    @Autowired
    protected DatabaseFacade dbf;


    private static final CLogger logger = Utils.getLogger(ExternalPrimaryStorageSpaceCapacityHelper.class);

    private String primaryStorageUuid;
    private String spaceName;
    private Map<String, ExternalPrimaryStorageSpaceVO> storageSpacesByUrl;

    // TODO: use factory to create helper, diff capabilities helper for diff types
    @Deprecated
    public ExternalPrimaryStorageSpaceCapacityHelper(ExternalPrimaryStorageVO ps) {
        super(ps);
        this.primaryStorageUuid = ps.getUuid();
        this.spaceName = ps.getIdentity();
    }

    // TODO: use factory to create helper, diff capabilities helper for diff types
    @Deprecated
    public ExternalPrimaryStorageSpaceCapacityHelper(String psUuid, String identity) {
        super(psUuid, identity);
        this.primaryStorageUuid = psUuid;
        spaceName = identity;
    }

    protected void updateStorageSpace(StorageCapacity cap) {
        if (cap.getCapacitiesByLocationUrl() == null || cap.getCapacitiesByLocationUrl().isEmpty()) {
            return;
        }

        List<ExternalPrimaryStorageSpaceVO> beforeSpaces = Q.New(ExternalPrimaryStorageSpaceVO.class)
                .eq(ExternalPrimaryStorageSpaceVO_.primaryStorageUuid, primaryStorageUuid)
                .list();

        Map<String, ExternalPrimaryStorageSpaceVO> beforeSpacesByUrl = beforeSpaces.stream()
                .collect(Collectors.toMap(ExternalPrimaryStorageSpaceVO::getLocationUrl, it -> it));
        List<ExternalPrimaryStorageSpaceVO> toPersistSpaces = new ArrayList<>();
        List<ExternalPrimaryStorageSpaceVO> toRemoveSpaces = new ArrayList<>();

        for (Map.Entry<String, StorageCapacity.Capacity> e : cap.getCapacitiesByLocationUrl().entrySet()) {
            String locationUrl = e.getKey();
            StorageCapacity.Capacity capacity = e.getValue();
            ExternalPrimaryStorageSpaceVO spaceVO = beforeSpacesByUrl.get(locationUrl);
            if (spaceVO == null) {
                spaceVO = new ExternalPrimaryStorageSpaceVO();
                spaceVO.setUuid(Platform.getUuidFromBytes((primaryStorageUuid + locationUrl).getBytes()));
                spaceVO.setPrimaryStorageUuid(primaryStorageUuid);
                spaceVO.setLocationUrl(locationUrl.replaceAll("/$", ""));
                spaceVO.setTotalPhysicalCapacity(capacity.total);
                spaceVO.setAvailablePhysicalCapacity(capacity.available);
                spaceVO.setTotalCapacity(capacity.total);
                spaceVO.setAvailableCapacity(capacity.available);
                toPersistSpaces.add(spaceVO);
            } else {
                spaceVO.setTotalPhysicalCapacity(capacity.total);
                spaceVO.setAvailablePhysicalCapacity(capacity.available);
                spaceVO.setTotalCapacity(capacity.total);
            }
        }

        for (Iterator<Map.Entry<String, ExternalPrimaryStorageSpaceVO>> it = beforeSpacesByUrl.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, ExternalPrimaryStorageSpaceVO> entry = it.next();
            if (!cap.getCapacitiesByLocationUrl().containsKey(entry.getKey())) {
                toRemoveSpaces.add(entry.getValue());
                it.remove();
            }
        }

        if (!toRemoveSpaces.isEmpty()) {
            dbf.removeCollection(toRemoveSpaces, ExternalPrimaryStorageSpaceVO.class);
        }
        if (!toPersistSpaces.isEmpty()) {
            dbf.persistCollection(toPersistSpaces);
        }
        dbf.updateCollection(beforeSpacesByUrl.values());
    }

    private ExternalPrimaryStorageSpaceVO getSpaceFromInstallUrl(String installPath) {
        String spaceUrl = getLocationSpaceUrl(installPath);
        return getStorageSpacesByUrl().get(spaceUrl);
    }

    @DeadlockAutoRestart
    public void releaseAvailableCapWithRatio(String installPath, long size) {
        long ratioSize = ratioMgr.calculateByRatio(primaryStorageUuid, size);
        _release(installPath, ratioSize);
    }

    @DeadlockAutoRestart
    public void releaseAvailableCapacity(String installPath, long size) {
        _release(installPath, size);
    }

    @Transactional
    protected void _release(String installPath, long size) {
        if (installPath == null) {
            logger.debug(String.format("no install path found, skip release %s capacity", spaceName));
            return;
        }

        ExternalPrimaryStorageSpaceVO space = getSpaceFromInstallUrl(installPath);
        space.setAvailableCapacity(space.getAvailableCapacity() + size);
        if (space.getAvailableCapacity() > space.getTotalPhysicalCapacity()) {
            logger.warn(String.format("invalid space[locationUrl:%s] capacity after release size %s, available capacity[%s] > total capacity[%s], " +
                            "try to reconnect ps to recalculate pool capacity",
                    space.getLocationUrl(), size, space.getAvailableCapacity(), space.getTotalPhysicalCapacity()));
        }

        dbf.getEntityManager().merge(space);
        logger.debug(String.format("ps space [%s] release capacity: %s, updated: %s",
                space.getLocationUrl(), size, space.getAvailableCapacity()));
    }

    @DeadlockAutoRestart
    public long reserveAvailableCapacity(String installPath, long size) {
        return _reserve(installPath, size);
    }

    @Transactional
    protected long _reserve(String installPath, long size) {
        if (installPath == null) {
            logger.debug(String.format("no install path found, skip reserve %s capacity", spaceName));
            return 0;
        }


        ExternalPrimaryStorageSpaceVO space = getSpaceFromInstallUrl(installPath);
        long originAvailableCapacity = space.getAvailableCapacity();
        if (originAvailableCapacity < size) {
            throw new OperationFailureException(operr(ORG_ZSTACK_STORAGE_ADDON_PRIMARY_10003, "required space[locationUrl:%s] cannot satisfy conditions [availableSize > %s bytes], " +
                    "current available size %s", space.getLocationUrl(), size, originAvailableCapacity));
        }

        space.setAvailableCapacity(space.getAvailableCapacity() - size);
        dbf.getEntityManager().merge(space);

        logger.debug(String.format("%s[%s] reserve capacity: %s, origin: %s, updated: %s",
                spaceName, space.getLocationUrl(), size, originAvailableCapacity, space.getAvailableCapacity()));
        return originAvailableCapacity;
    }

    public void recalculateAvailableCapacity() {
        Map<String, ExternalPrimaryStorageSpaceVO> spacesByUrl = getStorageSpacesByUrl();
        Map<String, Long> usedCapBySpaceUrl = spacesByUrl.keySet().stream()
                .collect(Collectors.toMap(url -> url, url -> 0L ));

        StopWatch watch = Utils.getStopWatch();
        watch.start();

        // 1. calculate used capacity from volumes
        long total = Q.New(VolumeVO.class).eq(VolumeVO_.primaryStorageUuid, primaryStorageUuid).count();
        SQL.New("select vol from VolumeVO vol" +
                        " where primaryStorageUuid = :psUuid", VolumeVO.class)
                .param("psUuid", primaryStorageUuid)
                .limit(1000)
                .paginate(total, vos -> {
                    for (Object v : vos) {
                        VolumeVO vo = (VolumeVO) v;
                        if (vo.getInstallPath() != null) {
                            ExternalPrimaryStorageSpaceVO space = getSpaceFromInstallUrl(vo.getInstallPath());
                            usedCapBySpaceUrl.compute(space.getLocationUrl(), (k, usedCap) -> usedCap + vo.getSize());
                        }
                    }
                });

        for (String key : usedCapBySpaceUrl.keySet()) {
            usedCapBySpaceUrl.compute(key, (k, usedCap) -> ratioMgr.calculateByRatio(primaryStorageUuid, usedCap));
        }

        // 2. calculate used capacity from image caches
        total = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.primaryStorageUuid, primaryStorageUuid).count();
        SQL.New("select img from ImageCacheVO img" +
                        " where primaryStorageUuid = :psUuid", ImageCacheVO.class)
                .param("psUuid", primaryStorageUuid)
                .limit(1000)
                .paginate(total, imgs -> {
                    for (Object i : imgs) {
                        ImageCacheVO img = (ImageCacheVO) i;
                        if (img.getInstallUrl() != null) {
                            ExternalPrimaryStorageSpaceVO space = getSpaceFromInstallUrl(img.getInstallUrl());
                            usedCapBySpaceUrl.compute(space.getLocationUrl(), (k, usedCap) -> usedCap + img.getSize());
                        }
                    }
                });


        // 3. calculate used capacity from snapshots
        total = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.primaryStorageUuid, primaryStorageUuid).count();
        SQL.New("select snap from VolumeSnapshotVO snap" +
                        " where primaryStorageUuid = :psUuid", VolumeSnapshotVO.class)
                .param("psUuid", primaryStorageUuid)
                .limit(1000)
                .paginate(total, snaps -> {
                    for (Object s : snaps) {
                        VolumeSnapshotVO snap = (VolumeSnapshotVO) s;
                        if (snap.getPrimaryStorageInstallPath() != null) {
                            ExternalPrimaryStorageSpaceVO space = getSpaceFromInstallUrl(snap.getPrimaryStorageInstallPath());
                            usedCapBySpaceUrl.compute(space.getLocationUrl(), (k, usedCap) -> usedCap + snap.getSize());
                        }
                    }
                });

        watch.stop();
        logger.info(String.format("it takes %d ms to recalculate external primary storage [%s] space used capacity",
                watch.getLapse(), primaryStorageUuid));

        // 4. update available capacity
        for (Map.Entry<String, ExternalPrimaryStorageSpaceVO> e : spacesByUrl.entrySet()) {
            String url = e.getKey();
            ExternalPrimaryStorageSpaceVO space = e.getValue();
            long usedCap = usedCapBySpaceUrl.get(url);
            long availableCap = space.getTotalPhysicalCapacity() - usedCap;
            space.setAvailableCapacity(availableCap);
            logger.info(String.format("recalculated external primary storage [%s] space [%s] available capacity: %s, used capacity: %s, total capacity: %s",
                    primaryStorageUuid, url, availableCap, usedCap, space.getTotalPhysicalCapacity()));
        }

        dbf.updateCollection(spacesByUrl.values());
    }

    public boolean checkVirtualSizeByRatio(String requiredInstallUrl, long requiredSize) {
        ExternalPrimaryStorageSpaceVO space = getSpaceFromInstallUrl(requiredInstallUrl);
        return PrimaryStorageCapacityChecker.New(primaryStorageUuid,
                        space.getAvailableCapacity(), space.getTotalPhysicalCapacity(), space.getAvailablePhysicalCapacity())
                .checkRequiredSize(requiredSize);
    }

    public String findMostSuitableSpace(long requiredSize, Comparator<ExternalPrimaryStorageSpaceVO> comparator) {
        Map<String, ExternalPrimaryStorageSpaceVO> spacesByUrl = getStorageSpacesByUrl();
        List<ExternalPrimaryStorageSpaceVO> suitableSpaces = spacesByUrl.values().stream()
                .filter(space -> PrimaryStorageCapacityChecker.New(primaryStorageUuid,
                                space.getAvailableCapacity(), space.getTotalPhysicalCapacity(), space.getAvailablePhysicalCapacity())
                        .checkRequiredSize(requiredSize))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(suitableSpaces)) {
            return null;
        }

        suitableSpaces.sort(comparator);
        return suitableSpaces.get(0).getLocationUrl();
    }


}
