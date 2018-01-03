package org.zstack.storage.surfs.backup;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.storage.surfs.SurfsConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by  zhouhaiping 2017-08-23
 */
@Inventory(mappingVOClass = SurfsBackupStorageVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BackupStorageInventory.class, type = SurfsConstants.SURFS_BACKUP_STORAGE_TYPE)}
)
@ExpandedQueries({
    @ExpandedQuery(expandedField = "nodes", inventoryClass = SurfsBackupStorageNodeInventory.class,
    foreignKey = "uuid", expandedInventoryKey = "backupStorageUuid")
})
public class SurfsBackupStorageInventory extends BackupStorageInventory {
	private List<SurfsBackupStorageNodeInventory> nodes = new ArrayList<SurfsBackupStorageNodeInventory>();
	private String fsid;
    private String poolName;
    private Integer sshPort;

    public SurfsBackupStorageInventory(SurfsBackupStorageVO vo) {
        super(vo);
        nodes = SurfsBackupStorageNodeInventory.valueOf(vo.getNodes());
        fsid = vo.getFsid();
        poolName = vo.getPoolName();
    }

    public SurfsBackupStorageInventory() {
    }
    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }
    
    public String getFsid() {
        return fsid;
    }

    public void setFsid(String fsid) {
        this.fsid = fsid;
    }

    public static SurfsBackupStorageInventory valueOf(SurfsBackupStorageVO vo) {
        return new SurfsBackupStorageInventory(vo);
    }

    public static List<SurfsBackupStorageInventory> valueOf1(Collection<SurfsBackupStorageVO> vos) {
        List<SurfsBackupStorageInventory> invs = new ArrayList<SurfsBackupStorageInventory>();
        for (SurfsBackupStorageVO vo : vos) {
            invs.add(new SurfsBackupStorageInventory(vo));
        }

        return invs;
    }

    public List<SurfsBackupStorageNodeInventory> getNodes() {
        return nodes;
    }

    public void setNodes(List<SurfsBackupStorageNodeInventory> nodes) {
        this.nodes = nodes;
    }
}
