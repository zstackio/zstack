package org.zstack.storage.fusionstor.primary;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.storage.fusionstor.FusionstorConstants;
import org.zstack.storage.fusionstor.backup.FusionstorBackupStorageMonInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@Inventory(mappingVOClass = FusionstorPrimaryStorageVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = PrimaryStorageInventory.class, type = FusionstorConstants.FUSIONSTOR_PRIMARY_STORAGE_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "mons", inventoryClass = FusionstorPrimaryStorageMonInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "primaryStorageUuid")
})
public class FusionstorPrimaryStorageInventory extends PrimaryStorageInventory {
    private List<FusionstorPrimaryStorageMonInventory> mons;
    private String fsid;
    private String rootVolumePoolName;
    private String dataVolumePoolName;
    private String imageCachePoolName;

    public List<FusionstorPrimaryStorageMonInventory> getMons() {
        return mons;
    }

    public void setMons(List<FusionstorPrimaryStorageMonInventory> mons) {
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

    public FusionstorPrimaryStorageInventory() {
    }

    public FusionstorPrimaryStorageInventory(FusionstorPrimaryStorageVO vo) {
        super(vo);
        setMons(FusionstorPrimaryStorageMonInventory.valueOf(vo.getMons()));
        setFsid(vo.getFsid());
        rootVolumePoolName = vo.getRootVolumePoolName();
        dataVolumePoolName = vo.getDataVolumePoolName();
        imageCachePoolName = vo.getImageCachePoolName();
    }

    public static FusionstorPrimaryStorageInventory valueOf(FusionstorPrimaryStorageVO vo) {
        return new FusionstorPrimaryStorageInventory(vo);
    }

    public static List<FusionstorPrimaryStorageInventory> valueOf1(Collection<FusionstorPrimaryStorageVO> vos) {
        List<FusionstorPrimaryStorageInventory> invs = new ArrayList<FusionstorPrimaryStorageInventory>();
        for (FusionstorPrimaryStorageVO vo : vos) {
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
