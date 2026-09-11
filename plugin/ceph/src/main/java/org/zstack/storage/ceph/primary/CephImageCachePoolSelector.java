package org.zstack.storage.ceph.primary;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.storage.ceph.CephGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

final class CephImageCachePoolSelector {
    private static final String CEPH_INSTALL_URL_PREFIX = "ceph://";
    static final String SNAPSHOT_REUSE_POOL_NAME = "snapshot-reuse";

    private static final CLogger logger = Utils.getLogger(CephImageCachePoolSelector.class);

    private final String primaryStorageUuid;
    private final String defaultPoolName;

    CephImageCachePoolSelector(String primaryStorageUuid, String defaultPoolName) {
        this.primaryStorageUuid = primaryStorageUuid;
        this.defaultPoolName = defaultPoolName;
    }

    Selection select(ImageInventory image, String targetVolumeInstallUrl) {
        if (isSnapshotReuseImage(image)) {
            return new Selection(CephImageCachePoolStrategy.DefaultImageCachePool,
                    SNAPSHOT_REUSE_POOL_NAME, findSnapshotReuseImageCache(image));
        }

        return select(image.getUuid(), targetVolumeInstallUrl, getStrategy());
    }

    Selection select(ImageInventory image, String targetVolumeInstallUrl, CephImageCachePoolStrategy strategy) {
        if (isSnapshotReuseImage(image)) {
            return new Selection(strategy, SNAPSHOT_REUSE_POOL_NAME, findSnapshotReuseImageCache(image));
        }

        return select(image.getUuid(), targetVolumeInstallUrl, strategy);
    }

    Selection select(String imageUuid, String targetVolumeInstallUrl) {
        return select(imageUuid, targetVolumeInstallUrl, getStrategy());
    }

    private Selection select(String imageUuid, String targetVolumeInstallUrl, CephImageCachePoolStrategy strategy) {
        List<ImageCacheVO> caches = listImageCaches(imageUuid);
        String poolName = selectPool(targetVolumeInstallUrl, caches, strategy);
        return new Selection(strategy, poolName, findCacheInPool(caches, poolName));
    }

    private Selection selectInPool(String imageUuid, String poolName, CephImageCachePoolStrategy strategy) {
        return new Selection(strategy, poolName, findCacheInPool(listImageCaches(imageUuid), poolName));
    }

    Selection selectInPool(ImageInventory image, String poolName, CephImageCachePoolStrategy strategy) {
        if (isSnapshotReuseImage(image)) {
            return new Selection(strategy, poolName, findSnapshotReuseImageCache(image));
        }

        return selectInPool(image.getUuid(), poolName, strategy);
    }

    List<ImageCacheVO> listImageCaches(String imageUuid) {
        return Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(ImageCacheVO_.imageUuid, imageUuid)
                .list();
    }

    static boolean isCephImageCacheRecord(ImageCacheVO cache) {
        return cache != null && cache.getInstallUrl() != null && cache.getInstallUrl().startsWith(CEPH_INSTALL_URL_PREFIX);
    }

    static boolean isSnapshotReuseImage(ImageInventory image) {
        return image != null && StringUtils.startsWith(image.getUrl(), ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA);
    }

    static String getPoolName(String installUrl) {
        if (StringUtils.isBlank(installUrl) || !installUrl.startsWith(CEPH_INSTALL_URL_PREFIX)) {
            return null;
        }

        String path = installUrl.substring(CEPH_INSTALL_URL_PREFIX.length());
        int index = path.indexOf("/");
        return index > 0 ? path.substring(0, index) : null;
    }

    private CephImageCachePoolStrategy getStrategy() {
        String strategy = CephGlobalConfig.IMAGE_CACHE_POOL_STRATEGY.value(String.class);
        try {
            return CephImageCachePoolStrategy.valueOf(strategy);
        } catch (RuntimeException e) {
            logger.warn(String.format("invalid ceph image cache pool strategy[%s], use DefaultImageCachePool", strategy));
            return CephImageCachePoolStrategy.DefaultImageCachePool;
        }
    }

    private String selectPool(String targetVolumeInstallUrl, List<ImageCacheVO> caches,
                              CephImageCachePoolStrategy strategy) {
        if (strategy == CephImageCachePoolStrategy.PreferVolumePool) {
            String targetPoolName = getPoolName(targetVolumeInstallUrl);
            return hasImageCachePoolRole(targetPoolName) ? targetPoolName : defaultPoolName;
        }

        if (strategy == CephImageCachePoolStrategy.PreferExistingCache) {
            boolean defaultPoolCacheExists = caches.stream()
                    .anyMatch(c -> defaultPoolName.equals(getPoolName(c.getInstallUrl())));
            if (defaultPoolCacheExists) {
                return defaultPoolName;
            }

            return caches.stream()
                    .map(c -> getPoolName(c.getInstallUrl()))
                    .filter(StringUtils::isNotBlank)
                    .findFirst()
                    .orElse(defaultPoolName);
        }

        return defaultPoolName;
    }

    private ImageCacheVO findCacheInPool(List<ImageCacheVO> caches, String poolName) {
        return caches.stream()
                .filter(c -> poolName.equals(getPoolName(c.getInstallUrl())))
                .findFirst()
                .orElse(null);
    }

    private ImageCacheVO findSnapshotReuseImageCache(ImageInventory image) {
        return Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(ImageCacheVO_.imageUuid, image.getUuid())
                .eq(ImageCacheVO_.installUrl, image.getUrl())
                .find();
    }

    private boolean hasImageCachePoolRole(String poolName) {
        if (StringUtils.isBlank(poolName)) {
            return false;
        }

        return Q.New(CephPrimaryStoragePoolVO.class)
                .eq(CephPrimaryStoragePoolVO_.primaryStorageUuid, primaryStorageUuid)
                .eq(CephPrimaryStoragePoolVO_.poolName, poolName)
                .eq(CephPrimaryStoragePoolVO_.type, CephPrimaryStoragePoolType.ImageCache.toString())
                .isExists();
    }

    static final class Selection {
        final CephImageCachePoolStrategy strategy;
        final String poolName;
        final ImageCacheVO cache;

        Selection(CephImageCachePoolStrategy strategy, String poolName, ImageCacheVO cache) {
            this.strategy = strategy;
            this.poolName = poolName;
            this.cache = cache;
        }
    }
}
