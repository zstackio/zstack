package org.zstack.storage.ceph.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.ceph.CephCapacityUpdateExtensionPoint;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.ceph.MonUri;

import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

/**
 * Created by frank on 7/27/2015.
 */
public class CephBackupStorageFactory implements BackupStorageFactory, CephCapacityUpdateExtensionPoint {
    @Autowired
    private DatabaseFacade dbf;

    public static final BackupStorageType type = new BackupStorageType(CephConstants.CEPH_BACKUP_STORAGE_TYPE);

    @Override
    public BackupStorageType getBackupStorageType() {
        return type;
    }

    @Override
    @Transactional
    public BackupStorageInventory createBackupStorage(final BackupStorageVO vo, APIAddBackupStorageMsg msg) {
        APIAddCephBackupStorageMsg cmsg = (APIAddCephBackupStorageMsg)msg;

        CephBackupStorageVO cvo = new CephBackupStorageVO(vo);
        cvo.setType(CephConstants.CEPH_BACKUP_STORAGE_TYPE);

        dbf.getEntityManager().persist(cvo);

        for (String url : cmsg.getMonUrls()) {
            CephBackupStorageMonVO monvo = new CephBackupStorageMonVO();
            MonUri uri = new MonUri(url);
            monvo.setUuid(Platform.getUuid());
            monvo.setStatus(MonStatus.Connecting);
            monvo.setHostname(uri.getHostname());
            monvo.setSshUsername(uri.getSshUsername());
            monvo.setSshPassword(uri.getSshPassword());
            monvo.setBackupStorageUuid(cvo.getUuid());
            dbf.getEntityManager().persist(monvo);
        }

        return BackupStorageInventory.valueOf(cvo);
    }

    @Override
    public BackupStorage getBackupStorage(BackupStorageVO vo) {
        CephBackupStorageVO cvo = dbf.findByUuid(vo.getUuid(), CephBackupStorageVO.class);
        return new CephBackupStorageBase(cvo);
    }

    @Override
    public BackupStorageInventory reload(String uuid) {
        return CephBackupStorageInventory.valueOf(dbf.findByUuid(uuid, CephBackupStorageVO.class));
    }

    @Override
    @Transactional
    public void update(String fsid, long total, long avail) {
        String sql = "select c from CephBackupStorageVO c where c.fsid = :fsid";
        TypedQuery<CephBackupStorageVO> q = dbf.getEntityManager().createQuery(sql, CephBackupStorageVO.class);
        q.setParameter("fsid", fsid);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        try {
            CephBackupStorageVO vo = q.getSingleResult();
            vo.setTotalCapacity(total);
            vo.setAvailableCapacity(avail);
            dbf.getEntityManager().merge(vo);
        } catch (EmptyResultDataAccessException e) {
            return;
        }
    }
}
