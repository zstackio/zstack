package org.zstack.storage.primary.local;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.ImageCacheCleanupDetails.ImageCacheCleanupInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.ImageCacheCleaner;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CacheInstallPath;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/7/20.
 */
public class LocalStorageImageCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LocalStorageImageCleaner.class);

    @Override
    public void managementNodeReady() {
        startGC();
    }

    @Override
    protected String getPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }

    @Transactional
    protected List<ImageCacheShadowVO> createShadowImageCacheVOsForNewDeletedAndOld(String psUUid) {
        List<Long> staleImageCacheIds = getStaleImageCacheIds(psUUid);
        if (staleImageCacheIds == null || staleImageCacheIds.isEmpty()) {
            return null;
        }

        String sql = "select c from ImageCacheVO c where c.id in (:ids)";
        TypedQuery<ImageCacheVO> cq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        cq.setParameter("ids", staleImageCacheIds);
        List<ImageCacheVO> deleted = cq.getResultList();

        Map<String, List<ImageCacheVO>> refMap = new HashMap<String, List<ImageCacheVO>>();
        for (ImageCacheVO c : deleted) {
            CacheInstallPath p = new CacheInstallPath();
            p.fullPath = c.getInstallUrl();
            p.disassemble();
            String hostUuid = p.hostUuid;

            List<ImageCacheVO> refs = refMap.get(hostUuid);
            if (refs == null) {
                refs = new ArrayList<ImageCacheVO>();
                refMap.put(hostUuid, refs);
            }
            refs.add(c);
        }

        List<ImageCacheVO> stale = new ArrayList<ImageCacheVO>();
        for (Map.Entry<String, List<ImageCacheVO>> e : refMap.entrySet()) {
            String hostUuid = e.getKey();
            List<ImageCacheVO> refs = e.getValue();
            List<Long> cacheIds = CollectionUtils.transformToList(refs, new Function<Long, ImageCacheVO>() {
                @Override
                public Long call(ImageCacheVO arg) {
                    return arg.getId();
                }
            });

            sql = "select c from ImageCacheVO c where c.imageUuid not in (select vol.rootImageUuid from VolumeVO vol, LocalStorageResourceRefVO ref" +
                    " where vol.uuid = ref.resourceUuid and ref.resourceType = :rtype and ref.hostUuid = :huuid) and c.id in (:ids)";
            cq = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
            cq.setParameter("rtype", VolumeVO.class.getSimpleName());
            cq.setParameter("huuid", hostUuid);
            cq.setParameter("ids", cacheIds);
            stale.addAll(cq.getResultList());
        }

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

    @Override
    public void cleanup(String psUuid, final ReturnValueCompletion<ImageCacheCleanupDetails> completion) {
        final ImageCacheCleanupDetails details = new ImageCacheCleanupDetails();
        details.setPrimaryStorageType(getPrimaryStorageType());

        List<ImageCacheShadowVO> shadowVOs = createShadowImageCacheVOs(psUuid);
        if (shadowVOs == null || shadowVOs.isEmpty()) {
            details.setNumberOfCleanedImageCache(0);
            completion.success(details);
            return;
        }

        List<ImageCacheShadowVO> ours = new ArrayList<ImageCacheShadowVO>();
        for (final ImageCacheShadowVO vo : shadowVOs) {
            if (destMaker.isManagedByUs(vo.getImageUuid())) {
                ours.add(vo);
            }
        }

        if (ours.isEmpty()) {
            details.setNumberOfCleanedImageCache(0);
            completion.success(details);
            return;
        }

        final AsyncLatch latch = new AsyncLatch(ours.size(), new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success(details);
            }
        });

        for (final ImageCacheShadowVO vo : ours) {
            CacheInstallPath p = new CacheInstallPath();
            p.fullPath = vo.getInstallUrl();
            p.disassemble();

            LocalStorageDeleteImageCacheOnPrimaryStorageMsg msg = new LocalStorageDeleteImageCacheOnPrimaryStorageMsg();
            msg.setHostUuid(p.hostUuid);
            msg.setImageUuid(vo.getImageUuid());
            msg.setInstallPath(p.installPath);
            msg.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, vo.getPrimaryStorageUuid());
            bus.send(msg, new CloudBusCallBack(latch) {
                @Override
                public void run(MessageReply reply) {
                    ImageCacheCleanupInventory inv = new ImageCacheCleanupInventory();
                    inv.setInventory(ImageCacheInventory.valueOf(vo.toImageCacheVO()));
                    details.addCleanupInventory(inv);

                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to delete the stale image cache[%s] on the primary storage[%s], %s," +
                                "will re-try later", vo.getInstallUrl(), vo.getPrimaryStorageUuid(), reply.getError()));
                        inv.setError(reply.getError());
                    } else {
                        logger.debug(String.format("successfully deleted the stale image cache[%s] on the primary storage[%s]",
                                vo.getInstallUrl(), vo.getPrimaryStorageUuid()));
                        details.increaseNumberOfCleanedImageCache();
                        dbf.remove(vo);
                    }

                    latch.ack();
                }
            });
        }
    }
}
