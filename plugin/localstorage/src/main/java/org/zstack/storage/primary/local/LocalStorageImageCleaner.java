package org.zstack.storage.primary.local;

import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.thread.SyncTask;
import org.zstack.header.host.HostVO;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.ImageCacheShadowVO;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
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
                    " where vol.uuid = ref.resourceUuid and ref.resourceType = :rtype and ref.hostUuid = :huuid and vol.rootImageUuid is not null) and c.id in (:ids)";
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

        sql = "select s from ImageCacheShadowVO s, PrimaryStorageVO p where p.uuid = s.primaryStorageUuid and p.type = :ptype";
        TypedQuery<ImageCacheShadowVO> sq = dbf.getEntityManager().createQuery(sql, ImageCacheShadowVO.class);
        sq.setParameter("ptype", getPrimaryStorageType());
        return sq.getResultList();
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

            CacheInstallPath p = new CacheInstallPath();
            p.fullPath = vo.getInstallUrl();
            p.disassemble();

            if (!dbf.isExist(p.hostUuid, HostVO.class)){
                dbf.removeByPrimaryKey(vo.getId(), ImageCacheShadowVO.class);
                continue;
            }
            LocalStorageDeleteImageCacheOnPrimaryStorageMsg msg = new LocalStorageDeleteImageCacheOnPrimaryStorageMsg();
            msg.setHostUuid(p.hostUuid);
            msg.setImageUuid(vo.getImageUuid());
            msg.setInstallPath(p.installPath);
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

    @Override
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
}
