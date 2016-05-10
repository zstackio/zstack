package org.zstack.storage.primary;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.EntityEvent;
import org.zstack.core.db.EntityLifeCycleCallback;
import org.zstack.header.Component;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeAO;
import org.zstack.header.volume.VolumeVO;

/**
 * Created by xing5 on 2016/5/10.
 */
public class DiskCapacityTracer implements Component {
    private static Logger logger = Logger.getLogger("org.zstack.storage.primary.DiskCapacityTracer");

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public boolean start() {
        if (!PrimaryStorageGlobalProperty.CAPACITY_TRACKER_ON) {
            return true;
        }

        dbf.installEntityLifeCycleCallback(VolumeVO.class, EntityEvent.POST_PERSIST, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeVO vol = (VolumeVO) o;
                if (vol.getSize() != 0) {
                    logger.debug(String.format("[Volume:Create][name=%s, uuid=%s, type=%s]: %s",
                            vol.getName(), vol.getUuid(), vol.getType(), vol.getSize()));
                }
            }
        });
        dbf.installEntityLifeCycleCallback(VolumeVO.class, EntityEvent.POST_UPDATE, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeVO vol = (VolumeVO) o;
                VolumeAO pre = vol.getShadow();
                if (pre.getSize() != vol.getSize()) {
                    logger.debug(String.format("[Volume:Update][name=%s, uuid=%s, type=%s]: %s --> %s",
                            vol.getName(), vol.getUuid(), vol.getType(), pre.getSize(), vol.getSize()));
                }
            }
        });
        dbf.installEntityLifeCycleCallback(ImageCacheVO.class, EntityEvent.POST_PERSIST, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                ImageCacheVO img = (ImageCacheVO) o;
                logger.debug(String.format("[ImageCache:Create][uuid=%s]: %s", img.getImageUuid(), img.getSize()));
            }
        });
        dbf.installEntityLifeCycleCallback(VolumeSnapshotVO.class, EntityEvent.POST_PERSIST, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeSnapshotVO s = (VolumeSnapshotVO) o;
                logger.debug(String.format("[VolumeSnapshot:Create][name=%s, uuid=%s]: %s", s.getName(), s.getUuid(), s.getSize()));
            }
        });
        dbf.installEntityLifeCycleCallback(PrimaryStorageCapacityVO.class, EntityEvent.POST_UPDATE, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                PrimaryStorageCapacityVO c = (PrimaryStorageCapacityVO) o;
                PrimaryStorageCapacityVO pre = c.getShadow();
                if (c.getAvailableCapacity() != pre.getAvailableCapacity()) {
                    logger.debug(String.format("[PrimaryStorageCapacity:Change][uuid=%s]: %s --> %s", pre.getUuid(), pre.getAvailableCapacity(), pre.getAvailableCapacity()));
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
