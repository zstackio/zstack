package org.zstack.storage.ceph.primary;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.config.GlobalConfig;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.storage.primary.ImageCacheShadowVO;
import org.zstack.header.storage.primary.ImageCacheVO;
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
public class CephImageCacheCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
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
    protected List<ImageCacheShadowVO> createShadowImageCacheVOs() {
        String sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri, ImageEO i where i.uuid = c.imageUuid and i.deleted is not null and pri.type = :ptype";
        TypedQuery<Long> cq = dbf.getEntityManager().createQuery(sql, Long.class);
        cq.setParameter("ptype", getPrimaryStorageType());
        List<Long> deleted = cq.getResultList();

        sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri where c.imageUuid not in (select img.uuid from ImageVO img) and" +
                " c.primaryStorageUuid = pri.uuid and pri.type = :psType";

        cq = dbf.getEntityManager().createQuery(sql, Long.class);
        cq.setParameter("psType", getPrimaryStorageType());
        deleted.addAll(cq.getResultList());

        if (deleted.isEmpty()) {
            return null;
        }

        sql = "select ref.imageCacheId from ImageCacheVolumeRefVO ref where ref.imageCacheId in (:ids)";
        TypedQuery<Long> refq = dbf.getEntityManager().createQuery(sql, Long.class);
        refq.setParameter("ids", deleted);
        List<Long> existing = refq.getResultList();

        deleted.removeAll(existing);

        if (deleted.isEmpty()) {
            return null;
        }

        sql = "select c from ImageCacheVO c where c.id in (:ids)";
        TypedQuery<ImageCacheVO> fq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        fq.setParameter("ids", deleted);
        List<ImageCacheVO> stale = fq.getResultList();

        logger.debug(String.format("found %s stale images in cache on the primary storage[type:%s], they are about to be cleaned up",
                stale.size(), getPrimaryStorageType()));

        for (ImageCacheVO vo : stale) {
            dbf.getEntityManager().persist(new ImageCacheShadowVO(vo));
            dbf.getEntityManager().remove(vo);
        }

        sql = "select s from ImageCacheShadowVO s";
        TypedQuery<ImageCacheShadowVO> sq = dbf.getEntityManager().createQuery(sql, ImageCacheShadowVO.class);
        return sq.getResultList();
    }

    @Override
    public void managementNodeReady() {
        startGC();
    }
}
