package org.zstack.storage.ceph.backup;

import org.zstack.header.search.Inventory;
import org.zstack.header.storage.backup.BackupStorageInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
@Inventory(mappingVOClass = CephBackupStorageVO.class, collectionValueOfMethod = "valueOf1")
public class CephBackupStorageInventory extends BackupStorageInventory {
    private List<CephBackupStorageMonInventory> mons = new ArrayList<CephBackupStorageMonInventory>();

    public CephBackupStorageInventory(CephBackupStorageVO vo) {
        super(vo);
        mons = CephBackupStorageMonInventory.valueOf(vo.getMons());
    }

    public CephBackupStorageInventory() {
    }

    public static CephBackupStorageInventory valueOf(CephBackupStorageVO vo) {
        return new CephBackupStorageInventory(vo);
    }

    public static List<CephBackupStorageInventory> valueOf1(Collection<CephBackupStorageVO> vos) {
        List<CephBackupStorageInventory> invs = new ArrayList<CephBackupStorageInventory>();
        for (CephBackupStorageVO vo : vos) {
            invs.add(new CephBackupStorageInventory(vo));
        }

        return invs;
    }

    public List<CephBackupStorageMonInventory> getMons() {
        return mons;
    }

    public void setMons(List<CephBackupStorageMonInventory> mons) {
        this.mons = mons;
    }
}
