package org.zstack.storage.ceph.primary;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.storage.ceph.backup.CephBackupStorageMonInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@Inventory(mappingVOClass = CephPrimaryStorageVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = PrimaryStorageInventory.class, type = CephConstants.CEPH_PRIMARY_STORAGE_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "mons", inventoryClass = CephPrimaryStorageMonInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "primaryStorageUuid")
})
public class CephPrimaryStorageInventory extends PrimaryStorageInventory {
    private List<CephPrimaryStorageMonInventory> mons;
    private String fsid;
    private String rootVolumePoolName;
    private String dataVolumePoolName;
    private String imageCachePoolName;

    public List<CephPrimaryStorageMonInventory> getMons() {
        return mons;
    }

    public void setMons(List<CephPrimaryStorageMonInventory> mons) {
        this.mons = mons;
    }

    public String getRootVolumePoolName() {
        return rootVolumePoolName;
    }

    public void setRootVolumePoolName(String rootVolumePoolName) {
        this.rootVolumePoolName = rootVolumePoolName;
    }

    public String getDataVolumePoolName() {
        return dataVolumePoolName;
    }

    public void setDataVolumePoolName(String dataVolumePoolName) {
        this.dataVolumePoolName = dataVolumePoolName;
    }

    public String getImageCachePoolName() {
        return imageCachePoolName;
    }

    public void setImageCachePoolName(String imageCachePoolName) {
        this.imageCachePoolName = imageCachePoolName;
    }

    public CephPrimaryStorageInventory() {
    }

    public CephPrimaryStorageInventory(CephPrimaryStorageVO vo) {
        super(vo);
        setMons(CephPrimaryStorageMonInventory.valueOf(vo.getMons()));
        setFsid(vo.getFsid());
        rootVolumePoolName = vo.getRootVolumePoolName();
        dataVolumePoolName = vo.getDataVolumePoolName();
        imageCachePoolName = vo.getImageCachePoolName();
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

    public String getFsid() {
        return fsid;
    }

    public void setFsid(String fsid) {
        this.fsid = fsid;
    }
}
