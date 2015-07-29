package org.zstack.storage.ceph.primary;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@Inventory(mappingVOClass = CephPrimaryStorageMonVO.class)
public class CephPrimaryStorageMonInventory {
    private String hostname;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static CephPrimaryStorageMonInventory valueOf(CephPrimaryStorageMonVO vo) {
        CephPrimaryStorageMonInventory inv = new CephPrimaryStorageMonInventory();
        inv.setHostname(vo.getHostname());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<CephPrimaryStorageMonInventory> valueOf(Collection<CephPrimaryStorageMonVO> vos) {
        List<CephPrimaryStorageMonInventory> invs = new ArrayList<CephPrimaryStorageMonInventory>();
        for (CephPrimaryStorageMonVO vo : vos) {
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

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
