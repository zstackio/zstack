package org.zstack.storage.ceph.backup;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
@Inventory(mappingVOClass = CephBackupStorageMonVO.class)
public class CephBackupStorageMonInventory {
    private String hostname;

    public static CephBackupStorageMonInventory valueOf(CephBackupStorageMonVO vo) {
        CephBackupStorageMonInventory inv = new CephBackupStorageMonInventory();
        inv.setHostname(vo.getHostname());
        return inv;
    }

    public static List<CephBackupStorageMonInventory> valueOf(Collection<CephBackupStorageMonVO> vos) {
        List<CephBackupStorageMonInventory> invs = new ArrayList<CephBackupStorageMonInventory>();
        for (CephBackupStorageMonVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
