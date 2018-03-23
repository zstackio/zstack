package org.zstack.storage.surfs;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;

import javax.persistence.LockModeType;

/**
 * Created by zhouhaiping 2017-08-26
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SurfsCapacityUpdater {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    public void update(String fsid, long total, long avail) {
        update(fsid, total, avail, true);
    }

    @Transactional
    public void update(String fsid, long total, long avail, boolean updatedAnyway) {
        SurfsCapacityVO vo = dbf.getEntityManager().find(SurfsCapacityVO.class, fsid, LockModeType.PESSIMISTIC_WRITE);
        boolean updated = false;

        if (vo == null) {
            GLock lock = new GLock(String.format("surfs-%s", fsid), 120);
            lock.lock();
            try {
                vo = dbf.getEntityManager().find(SurfsCapacityVO.class, fsid, LockModeType.PESSIMISTIC_WRITE);
                if (vo == null) {
                    vo = new SurfsCapacityVO();
                    vo.setFsid(fsid);
                    vo.setTotalCapacity(total);
                    vo.setAvailableCapacity(avail);
                    dbf.getEntityManager().persist(vo);
                    updated = true;
                } else {
                    if (vo.getAvailableCapacity() != avail || vo.getTotalCapacity() != total) {
                        vo.setTotalCapacity(total);
                        vo.setAvailableCapacity(avail);
                        dbf.getEntityManager().merge(vo);
                        updated = true;
                    }
                }
            } finally {
                lock.unlock();
            }
        } else  {
            if (vo.getAvailableCapacity() != avail || vo.getTotalCapacity() != total) {
                vo.setTotalCapacity(total);
                vo.setAvailableCapacity(avail);
                dbf.getEntityManager().merge(vo);
                updated = true;
            }
        }

        if (updatedAnyway || updated) {
            for (SurfsCapacityUpdateExtensionPoint ext : pluginRgty.getExtensionList(SurfsCapacityUpdateExtensionPoint.class)) {
                ext.update(fsid, total, avail);
            }
        }
    }
}
