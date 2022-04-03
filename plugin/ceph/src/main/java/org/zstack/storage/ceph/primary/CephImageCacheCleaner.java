package org.zstack.storage.ceph.primary;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ExpungeImageExtensionPoint;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.backup.BackupStorageVO_;
import org.zstack.header.storage.primary.CleanUpImageCacheOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ImageCacheShadowVO;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.CephGlobalConfig;
import org.zstack.storage.primary.ImageCacheCleaner;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by xing5 on 2016/7/23.
 */
public class CephImageCacheCleaner extends ImageCacheCleaner implements ExpungeImageExtensionPoint, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(CephImageCacheCleaner.class);

    @Override
    protected String getPrimaryStorageType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    @Override
    protected GlobalConfig cleanupIntervalConfig() {
        return CephGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL;
    }

    @Transactional
    @Override
    protected List<ImageCacheShadowVO> createShadowImageCacheVOs(String psUuid) {
        List<Long> staleImageCacheIds = getStaleImageCacheIds(psUuid);
        if (staleImageCacheIds == null || staleImageCacheIds.isEmpty()) {
            return null;
        }

        String sql = "select ref.imageCacheId from ImageCacheVolumeRefVO ref where ref.imageCacheId in (:ids)";
        TypedQuery<Long> refq = dbf.getEntityManager().createQuery(sql, Long.class);
        refq.setParameter("ids", staleImageCacheIds);
        List<Long> existing = refq.getResultList();

        staleImageCacheIds.removeAll(existing);

        if (staleImageCacheIds.isEmpty()) {
            return null;
        }

        sql = "select c from ImageCacheVO c where c.id in (:ids)";
        TypedQuery<ImageCacheVO> fq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        fq.setParameter("ids", staleImageCacheIds);
        List<ImageCacheVO> stale = fq.getResultList();

        logger.debug(String.format("found %s stale images in cache on the primary storage[type:%s], they are about to be cleaned up",
                stale.size(), getPrimaryStorageType()));

        for (ImageCacheVO vo : stale) {
            dbf.getEntityManager().persist(new ImageCacheShadowVO(vo));
            dbf.getEntityManager().remove(vo);
        }

        sql = "select s from ImageCacheShadowVO s, PrimaryStorageVO p where p.uuid = s.primaryStorageUuid and p.type = :ptype";
        TypedQuery<ImageCacheShadowVO> sq = dbf.getEntityManager().createQuery(sql, ImageCacheShadowVO.class);
        sq.setParameter("ptype", getPrimaryStorageType());
        return sq.getResultList();
    }

    @Override
    public void managementNodeReady() {
        startGC();
    }

    @Override
    public void preExpungeImage(ImageInventory img) {
    }

    @Override
    public void beforeExpungeImage(ImageInventory img) {
    }

    private List<String> getPsUuidWithImageCache(String imgUuid) {
        String sql = "select c.primaryStorageUuid from ImageCacheVO c, PrimaryStorageVO p, VolumeVO v where c.imageUuid = :imgUuid " +
                "and c.primaryStorageUuid = p.uuid " +
                "and p.type = :type " +
                "and c.imageUuid != v.rootImageUuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("imgUuid" , imgUuid);
        q.setParameter("type", getPrimaryStorageType());
        return q.getResultList();
    }

    @Override
    public void afterExpungeImage(ImageInventory img, String imageBackupStorageUuid) {
        String bsType = Q.New(BackupStorageVO.class).select(BackupStorageVO_.type).eq(BackupStorageVO_.uuid, imageBackupStorageUuid).findValue();
        if (!CephConstants.CEPH_BACKUP_STORAGE_TYPE.equals(bsType)) {
            return;
        }

        List<String> psUuids = getPsUuidWithImageCache(img.getUuid());
        psUuids.forEach(psUuid -> {
            CleanUpImageCacheOnPrimaryStorageMsg msg = new CleanUpImageCacheOnPrimaryStorageMsg();
            msg.setUuid(psUuid);
            bus.makeLocalServiceId(msg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(msg);
        });
    }

    @Override
    public void failedToExpungeImage(ImageInventory img, ErrorCode err) {
    }
}
