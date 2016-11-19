package org.zstack.storage.fusionstor.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.fusionstor.*;
import org.zstack.tag.SystemTagCreator;

import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
public class FusionstorBackupStorageFactory implements BackupStorageFactory, FusionstorCapacityUpdateExtensionPoint, Component {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AnsibleFacade asf;

    public static final BackupStorageType type = new BackupStorageType(FusionstorConstants.FUSIONSTOR_BACKUP_STORAGE_TYPE);

    static {
        type.setOrder(799);
    }

    void init() {
        type.setPrimaryStorageFinder(new BackupStorageFindRelatedPrimaryStorage() {
            @Override
            @Transactional(readOnly = true)
            public List<String> findRelatedPrimaryStorage(String backupStorageUuid) {
                String sql = "select p.uuid from FusionstorPrimaryStorageVO p, FusionstorBackupStorageVO b where b.fsid = p.fsid" +
                        " and b.uuid = :buuid";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("buuid", backupStorageUuid);
                return q.getResultList();
            }
        });
    }

    @Override
    public BackupStorageType getBackupStorageType() {
        return type;
    }

    @Override
    @Transactional
    public BackupStorageInventory createBackupStorage(final BackupStorageVO vo, APIAddBackupStorageMsg msg) {
        APIAddFusionstorBackupStorageMsg cmsg = (APIAddFusionstorBackupStorageMsg)msg;

        FusionstorBackupStorageVO cvo = new FusionstorBackupStorageVO(vo);
        cvo.setType(FusionstorConstants.FUSIONSTOR_BACKUP_STORAGE_TYPE);
        String poolName = cmsg.getPoolName() == null ? String.format("bak-t-%s", vo.getUuid()) : cmsg.getPoolName();
        cvo.setPoolName(poolName);

        dbf.getEntityManager().persist(cvo);

        if (cmsg.getPoolName() != null) {
            SystemTagCreator creator = FusionstorSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }

        for (String url : cmsg.getMonUrls()) {
            FusionstorBackupStorageMonVO monvo = new FusionstorBackupStorageMonVO();
            MonUri uri = new MonUri(url);
            monvo.setUuid(Platform.getUuid());
            monvo.setStatus(MonStatus.Connecting);
            monvo.setHostname(uri.getHostname());
            monvo.setMonPort(uri.getMonPort());
            monvo.setSshPort(uri.getSshPort());
            monvo.setSshUsername(uri.getSshUsername());
            monvo.setSshPassword(uri.getSshPassword());
            monvo.setBackupStorageUuid(cvo.getUuid());
            dbf.getEntityManager().persist(monvo);
        }

        return BackupStorageInventory.valueOf(cvo);
    }

    @Override
    public BackupStorage getBackupStorage(BackupStorageVO vo) {
        FusionstorBackupStorageVO cvo = dbf.findByUuid(vo.getUuid(), FusionstorBackupStorageVO.class);
        return new FusionstorBackupStorageBase(cvo);
    }

    @Override
    public BackupStorageInventory reload(String uuid) {
        return FusionstorBackupStorageInventory.valueOf(dbf.findByUuid(uuid, FusionstorBackupStorageVO.class));
    }

    @Override
    @Transactional
    public void update(String fsid, long total, long avail) {
        String sql = "select c from FusionstorBackupStorageVO c where c.fsid = :fsid";
        TypedQuery<FusionstorBackupStorageVO> q = dbf.getEntityManager().createQuery(sql, FusionstorBackupStorageVO.class);
        q.setParameter("fsid", fsid);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        try {
            FusionstorBackupStorageVO vo = q.getSingleResult();
            vo.setTotalCapacity(total);
            vo.setAvailableCapacity(avail);
            dbf.getEntityManager().merge(vo);
        } catch (EmptyResultDataAccessException e) {
            return;
        }
    }

    @Override
    public boolean start() {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            asf.deployModule(FusionstorGlobalProperty.BACKUP_STORAGE_MODULE_PATH, FusionstorGlobalProperty.BACKUP_STORAGE_PLAYBOOK_NAME);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
