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
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeType;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
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
        List<ImageCacheShadowVO> shadowVOs = createShadowImageCacheVOs();
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
            bus.send(msg, new CloudBusCallBack() {
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
    protected List<ImageCacheShadowVO> createShadowImageCacheVOs() {
        String sql = "select count(*) from VolumeVO vol, PrimaryStorageVO pri where vol.primaryStorageUuid = pri.uuid" +
                " and vol.type = :volType and vol.rootImageUuid is null and pri.type = :psType";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("volType", VolumeType.Root);
        q.setParameter("psType", getPrimaryStorageType());
        Long count = q.getSingleResult();
        if (count != 0) {
            logger.warn(String.format("found %s volumes on the primary storage[type:%s] has NULL rootImageUuid. Please do following:\n" +
                    "1. zstack-ctl stop_node\n" +
                    "2. zstack-ctl start_node -DfixImageCacheUuid=true -DrootVolumeFindMissingImageUuid=true\n" +
                    "to fix the problem. For the data safety, we won't clean the image cache of the primary storage", count, getPrimaryStorageType()));
            return null;
        }

        sql = "select c from ImageCacheVO c, PrimaryStorageVO pri, ImageEO i where i.uuid = c.imageUuid and i.deleted is not null and pri.type = :ptype";
        TypedQuery<ImageCacheVO> cq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        cq.setParameter("ptype", getPrimaryStorageType());
        List<ImageCacheVO> deleted = cq.getResultList();

        sql = "select c from ImageCacheVO c, PrimaryStorageVO pri where c.imageUuid not in (select img.uuid from ImageVO img) and" +
                " c.primaryStorageUuid = pri.uuid and pri.type = :psType";

        cq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        cq.setParameter("psType", getPrimaryStorageType());
        deleted.addAll(cq.getResultList());

        if (deleted.isEmpty()) {
            return null;
        }

        List<String> deleteImageUuids = CollectionUtils.transformToList(deleted, new Function<String, ImageCacheVO>() {
            @Override
            public String call(ImageCacheVO arg) {
                return arg.getImageUuid();
            }
        });

        sql = "select c from ImageCacheVO c where c.imageUuid not in (select vol.rootImageUuid from VolumeVO vol) and c.imageUuid in (:uuids)";
        cq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        cq.setParameter("uuids", deleteImageUuids);
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

        sql = "select s from ImageCacheShadowVO s";
        TypedQuery<ImageCacheShadowVO> sq = dbf.getEntityManager().createQuery(sql, ImageCacheShadowVO.class);
        return sq.getResultList();
    }
}
