package org.zstack.storage.surfs.primary;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.surfs.SurfsConstants;
import org.zstack.storage.surfs.backup.SurfsBackupStorageNodeInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@Inventory(mappingVOClass = SurfsPrimaryStorageVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = PrimaryStorageInventory.class, type = SurfsConstants.SURFS_PRIMARY_STORAGE_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "nodes", inventoryClass = SurfsPrimaryStorageNodeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "primaryStorageUuid")
})
public class SurfsPrimaryStorageInventory extends PrimaryStorageInventory {
    private List<SurfsPrimaryStorageNodeInventory> nodes;
    private String fsid;
    private String rootVolumePoolName;
    private String dataVolumePoolName;
    private String imageCachePoolName;

    public List<SurfsPrimaryStorageNodeInventory> getNodes() {
        return nodes;
    }

    public void setNodes(List<SurfsPrimaryStorageNodeInventory> nodes) {
        this.nodes = nodes;
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

    public SurfsPrimaryStorageInventory() {
    }

    public SurfsPrimaryStorageInventory(SurfsPrimaryStorageVO vo) {
        super(vo);
        setNodes(SurfsPrimaryStorageNodeInventory.valueOf(vo.getNodes()));
        setFsid(vo.getFsid());
        rootVolumePoolName = vo.getRootVolumePoolName();
        dataVolumePoolName = vo.getDataVolumePoolName();
        imageCachePoolName = vo.getImageCachePoolName();
    }

    public static SurfsPrimaryStorageInventory valueOf(SurfsPrimaryStorageVO vo) {
        return new SurfsPrimaryStorageInventory(vo);
    }

    public static List<SurfsPrimaryStorageInventory> valueOf1(Collection<SurfsPrimaryStorageVO> vos) {
        List<SurfsPrimaryStorageInventory> invs = new ArrayList<SurfsPrimaryStorageInventory>();
        for (SurfsPrimaryStorageVO vo : vos) {
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
