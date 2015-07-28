package org.zstack.storage.ceph.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.MonStatus;
import org.zstack.storage.ceph.MonUri;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
public class CephBackupStorageFactory implements BackupStorageFactory {
    @Autowired
    private DatabaseFacade dbf;

    public static final BackupStorageType type = new BackupStorageType(CephConstants.CEPH_BACKUP_STORAGE_TYPE);

    @Override
    public BackupStorageType getBackupStorageType() {
        return type;
    }

    @Override
    public BackupStorageInventory createBackupStorage(final BackupStorageVO vo, APIAddBackupStorageMsg msg) {
        APIAddCephBackupStorageMsg cmsg = (APIAddCephBackupStorageMsg)msg;

        vo.setType(CephConstants.CEPH_BACKUP_STORAGE_TYPE);

        final List<CephBackupStorageMonVO> monVOs = new ArrayList<CephBackupStorageMonVO>();
        for (String url : cmsg.getMonUrls()) {
            CephBackupStorageMonVO monvo = new CephBackupStorageMonVO();
            MonUri uri = new MonUri(url);
            monvo.setUuid(Platform.getUuid());
            monvo.setStatus(MonStatus.Connecting);
            monvo.setHostname(uri.getHostname());
            monvo.setSshUsername(uri.getSshUsername());
            monvo.setSshPassword(uri.getSshPassword());
            monVOs.add(monvo);
        }

        new Runnable() {
            @Override
            @Transactional
            public void run() {
                dbf.getEntityManager().persist(vo);

                for (CephBackupStorageMonVO monvo : monVOs) {
                    monvo.setBackupStorageUuid(vo.getUuid());
                    dbf.getEntityManager().persist(monvo);
                }
            }
        }.run();

        return BackupStorageInventory.valueOf(vo);
    }

    @Override
    public BackupStorage getBackupStorage(BackupStorageVO vo) {
        return new CephBackupStorageBase(vo);
    }

    @Override
    public BackupStorageInventory reload(String uuid) {
        return CephBackupStorageInventory.valueOf(dbf.findByUuid(uuid, CephBackupStorageVO.class));
    }
}
