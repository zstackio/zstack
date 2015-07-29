package org.zstack.storage.ceph.primary;

import org.zstack.header.search.Inventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@Inventory(mappingVOClass = CephPrimaryStorageVO.class, collectionValueOfMethod = "valueOf1")
public class CephPrimaryStorageInventory extends PrimaryStorageInventory {
    private List<CephPrimaryStorageMonInventory> mons;

    public List<CephPrimaryStorageMonInventory> getMons() {
        return mons;
    }

    public void setMons(List<CephPrimaryStorageMonInventory> mons) {
        this.mons = mons;
    }

    public CephPrimaryStorageInventory() {
    }

    public CephPrimaryStorageInventory(CephPrimaryStorageVO vo) {
        super(vo);
        setMons(CephPrimaryStorageMonInventory.valueOf(vo.getMons()));
    }

    public static CephPrimaryStorageInventory valueOf(CephPrimaryStorageVO vo) {
        return new CephPrimaryStorageInventory(vo);
    }

    public static List<CephPrimaryStorageInventory> valueOf1(Collection<CephPrimaryStorageVO> vos) {
        List<CephPrimaryStorageInventory> invs = new ArrayList<CephPrimaryStorageInventory>();
        for (CephPrimaryStorageVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }
}
