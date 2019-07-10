package org.zstack.storage.surfs.backup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.ansible.AnsibleFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.Component;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.surfs.*;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;

import java.util.List;

/**
 * Created by zhouhaiping 2017-09-01
 */
public class SurfsBackupStorageFactory implements BackupStorageFactory, SurfsCapacityUpdateExtensionPoint, Component {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AnsibleFacade asf;

    public static final BackupStorageType type = new BackupStorageType(SurfsConstants.SURFS_BACKUP_STORAGE_TYPE);
    private static final CLogger logger = Utils.getLogger(SurfsBackupStorageFactory.class);
    
    static {
        type.setOrder(431);
    }

    void init() {
        type.setPrimaryStorageFinder(new BackupStorageFindRelatedPrimaryStorage() {
            @Override
            @Transactional(readOnly = true)
            public List<String> findRelatedPrimaryStorage(String backupStorageUuid) {
                String sql = "select p.uuid from SurfsPrimaryStorageVO p, SurfsBackupStorageVO b where b.fsid = p.fsid" +
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
        APIAddSurfsBackupStorageMsg cmsg = (APIAddSurfsBackupStorageMsg)msg;
        SurfsBackupStorageVO cvo = new SurfsBackupStorageVO(vo);
        cvo.setType(SurfsConstants.SURFS_BACKUP_STORAGE_TYPE);
        String poolName = cmsg.getPoolName() == null ? String.format("bak-t-%s", vo.getUuid()) : cmsg.getPoolName();
        cvo.setPoolName(poolName);

        dbf.getEntityManager().persist(cvo);

        if (cmsg.getPoolName() != null) {
            SystemTagCreator creator = SurfsSystemTags.PREDEFINED_BACKUP_STORAGE_POOL.newSystemTagCreator(cvo.getUuid());
            creator.ignoreIfExisting = true;
            creator.create();
        }
        for (String url : cmsg.getNodeUrls()) {
            SurfsBackupStorageNodeVO monvo = new SurfsBackupStorageNodeVO();
            NodeUri uri = new NodeUri(url);
            monvo.setUuid(Platform.getUuid());
            monvo.setStatus(NodeStatus.Connecting);
            monvo.setHostname(uri.getHostname());
            monvo.setNodeAddr(monvo.getHostname());
            monvo.setNodePort(uri.getNodePort());
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
        SurfsBackupStorageVO cvo = dbf.findByUuid(vo.getUuid(), SurfsBackupStorageVO.class);
        return new SurfsBackupStorageBase(cvo);
    }

    @Override
    public BackupStorageInventory reload(String uuid) {
        return SurfsBackupStorageInventory.valueOf(dbf.findByUuid(uuid, SurfsBackupStorageVO.class));
    }

    @Override
    @Transactional
    public void update(String fsid,long total, long avail) {
        String sql = "select c from SurfsBackupStorageVO c where c.fsid = :fsid";
        TypedQuery<SurfsBackupStorageVO> q = dbf.getEntityManager().createQuery(sql, SurfsBackupStorageVO.class);
        q.setParameter("fsid", fsid);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        try {
            SurfsBackupStorageVO vo = q.getSingleResult();
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
            asf.deployModule(SurfsGlobalProperty.BACKUP_STORAGE_MODULE_PATH, SurfsGlobalProperty.BACKUP_STORAGE_PLAYBOOK_NAME);
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
