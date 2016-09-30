package org.zstack.storage.fusionstor.backup;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.storage.fusionstor.FusionstorConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
@Inventory(mappingVOClass = FusionstorBackupStorageVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = BackupStorageInventory.class, type = FusionstorConstants.FUSIONSTOR_BACKUP_STORAGE_TYPE)}
)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "mons", inventoryClass = FusionstorBackupStorageMonInventory.class,
        foreignKey = "uuid", expandedInventoryKey = "backupStorageUuid")
})
public class FusionstorBackupStorageInventory extends BackupStorageInventory {
    private List<FusionstorBackupStorageMonInventory> mons = new ArrayList<FusionstorBackupStorageMonInventory>();
    private String fsid;
    private String poolName;
    private Integer sshPort;

    public FusionstorBackupStorageInventory(FusionstorBackupStorageVO vo) {
        super(vo);
        mons = FusionstorBackupStorageMonInventory.valueOf(vo.getMons());
        fsid = vo.getFsid();
        poolName = vo.getPoolName();
    }

    public FusionstorBackupStorageInventory() {
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

    public static FusionstorBackupStorageInventory valueOf(FusionstorBackupStorageVO vo) {
        return new FusionstorBackupStorageInventory(vo);
    }

    public static List<FusionstorBackupStorageInventory> valueOf1(Collection<FusionstorBackupStorageVO> vos) {
        List<FusionstorBackupStorageInventory> invs = new ArrayList<FusionstorBackupStorageInventory>();
        for (FusionstorBackupStorageVO vo : vos) {
            invs.add(new FusionstorBackupStorageInventory(vo));
        }

        return invs;
    }

    public List<FusionstorBackupStorageMonInventory> getMons() {
        return mons;
    }

    public void setMons(List<FusionstorBackupStorageMonInventory> mons) {
        this.mons = mons;
    }
}
