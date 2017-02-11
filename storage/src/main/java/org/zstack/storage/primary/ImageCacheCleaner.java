package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.DeleteImageCacheOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.ImageCacheShadowVO;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/7/18.
 */
public abstract class ImageCacheCleaner {
    private static final CLogger logger = Utils.getLogger(ImageCacheCleaner.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected ResourceDestinationMaker destMaker;

    protected Future<Void> gcThread;

    protected abstract String getPrimaryStorageType();

    protected void startGC() {
        cleanupIntervalConfig().installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                if (gcThread != null) {
                    gcThread.cancel(true);
                }

                startGCThread();
            }
        });

        startGCThread();
    }

    protected GlobalConfig cleanupIntervalConfig() {
        return PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL;
    }

    public void cleanup() {
        cleanup(null);
    }

    public void cleanup(String psUuid) {
        ImageCacheCleaner self = this;
        thdf.syncSubmit(new SyncTask<Void>() {
            @Override
            public Void call() throws Exception {
                doCleanup(psUuid);
                return null;
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }

            @Override
            public String getSyncSignature() {
                return self.getClass().getName();
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }
        });
    }

    protected void doCleanup(String psUuid) {
        List<ImageCacheShadowVO> shadowVOs = createShadowImageCacheVOs(psUuid);
        if (shadowVOs == null || shadowVOs.isEmpty()) {
            return;
        }

        for (final ImageCacheShadowVO vo : shadowVOs) {
            if (!destMaker.isManagedByUs(vo.getImageUuid())) {
                continue;
            }

            DeleteImageCacheOnPrimaryStorageMsg msg = new DeleteImageCacheOnPrimaryStorageMsg();
            msg.setImageUuid(vo.getImageUuid());
            msg.setInstallPath(vo.getInstallUrl());
            msg.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vo.getPrimaryStorageUuid());
            bus.send(msg, new CloudBusCallBack(null) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to delete the stale image cache[%s] on the primary storage[%s], %s," +
                                "will re-try later", vo.getInstallUrl(), vo.getPrimaryStorageUuid(), reply.getError()));
                        return;
                    }

                    logger.debug(String.format("successfully deleted the stale image cache[%s] on the primary storage[%s]",
                            vo.getInstallUrl(), vo.getPrimaryStorageUuid()));
                    dbf.remove(vo);
                }
            });
        }
    }

    private void startGCThread() {
        logger.debug(String.format("%s starts with the interval %s secs", this.getClass().getSimpleName(), PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL.value(Long.class)));

        gcThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return cleanupIntervalConfig().value(Long.class);
            }

            @Override
            public String getName() {
                return "image-cache-cleanup-thread";
            }

            @Override
            public void run() {
                cleanup();
            }
        });
    }

    @Transactional
    protected List<Long> getStaleImageCacheIds(String psUuid) {
        String sql;
        if (psUuid == null) {
            sql = "select count(*) from VolumeVO vol, PrimaryStorageVO pri where vol.primaryStorageUuid = pri.uuid" +
                    " and vol.type = :volType and vol.rootImageUuid is null and pri.type = :psType";
        } else {
            sql = "select count(*) from VolumeVO vol, PrimaryStorageVO pri where vol.primaryStorageUuid = pri.uuid" +
                    " and vol.type = :volType and vol.rootImageUuid is null and pri.type = :psType and pri.uuid = :psUuid";
        }

        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("volType", VolumeType.Root);
        q.setParameter("psType", getPrimaryStorageType());
        if (psUuid != null) {
            q.setParameter("psUuid", psUuid);
        }

        Long count = q.getSingleResult();
        if (count != 0) {
            logger.warn(String.format("found %s volumes on the primary storage[type:%s] has NULL rootImageUuid. Please do following:\n" +
                    "1. zstack-ctl stop_node\n" +
                    "2. zstack-ctl start_node -DfixImageCacheUuid=true -DrootVolumeFindMissingImageUuid=true\n" +
                    "to fix the problem. For the data safety, we won't clean the image cache of the primary storage", count, getPrimaryStorageType()));
            return null;
        }

        if (psUuid == null) {
            sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri, ImageEO i where c.primaryStorageUuid = pri.uuid and i.uuid = c.imageUuid and i.deleted is not null and pri.type = :ptype";
        } else  {
            sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri, ImageEO i where c.primaryStorageUuid = pri.uuid and i.uuid = c.imageUuid and i.deleted is not null and pri.type = :ptype and pri.uuid = :psUuid";
        }

        TypedQuery<Long> cq = dbf.getEntityManager().createQuery(sql, Long.class);
        cq.setParameter("ptype", getPrimaryStorageType());
        if (psUuid != null) {
            cq.setParameter("psUuid", psUuid);
        }
        List<Long> deleted = cq.getResultList();

        if (psUuid == null) {
            sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri where c.imageUuid not in (select img.uuid from ImageVO img) and" +
                    " c.primaryStorageUuid = pri.uuid and pri.type = :psType";
        } else {
            sql = "select c.id from ImageCacheVO c, PrimaryStorageVO pri where c.imageUuid not in (select img.uuid from ImageVO img) and" +
                    " c.primaryStorageUuid = pri.uuid and pri.type = :psType and pri.uuid = :psUuid";
        }

        cq = dbf.getEntityManager().createQuery(sql, Long.class);
        cq.setParameter("psType", getPrimaryStorageType());
        if (psUuid != null) {
            cq.setParameter("psUuid", psUuid);
        }
        deleted.addAll(cq.getResultList());

        if (deleted.isEmpty()) {
            return null;
        }

        return deleted;
    }

    @Transactional
    protected List<ImageCacheShadowVO> createShadowImageCacheVOsForNewDeletedAndOld(String psUuid) {
        List<Long> staleImageCacheIds = getStaleImageCacheIds(psUuid);
        if (staleImageCacheIds == null || staleImageCacheIds.isEmpty()) {
            return null;
        }

        String sql = "select c from ImageCacheVO c where c.imageUuid not in (select vol.rootImageUuid from VolumeVO vol where vol.rootImageUuid is not null) and c.id in (:ids)";
        TypedQuery<ImageCacheVO> cq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        cq.setParameter("ids", staleImageCacheIds);
        List<ImageCacheVO> stale = cq.getResultList();

        if (stale.isEmpty()) {
            return null;
        }

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

    @Transactional
    protected List<ImageCacheShadowVO> createShadowImageCacheVOs(String psUuid) {
        List<ImageCacheShadowVO> newDeletedAndOld = createShadowImageCacheVOsForNewDeletedAndOld(psUuid);
        if (newDeletedAndOld == null) {
            // no new deleted images, let's check if there any old that failed to be deleted last time
            String sql = "select s from ImageCacheShadowVO s, PrimaryStorageVO p where p.uuid = s.primaryStorageUuid and p.type = :ptype";
            TypedQuery<ImageCacheShadowVO> sq = dbf.getEntityManager().createQuery(sql, ImageCacheShadowVO.class);
            sq.setParameter("ptype", getPrimaryStorageType());
            return sq.getResultList();
        } else {
            return newDeletedAndOld;
        }
    }
}
