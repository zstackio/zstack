package org.zstack.storage.surfs.primary;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by zhouhaiping 2017-11-24
 */
@Inventory(mappingVOClass = SurfsPrimaryStoragePoolVO.class)
public class SurfsPrimaryStoragePoolInventory {
    private String uuid;
    private String primaryStorageUuid;
    private String poolName;
    private String aliasName;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String type;

    public static SurfsPrimaryStoragePoolInventory valueOf(SurfsPrimaryStoragePoolVO vo) {
        SurfsPrimaryStoragePoolInventory inv = new SurfsPrimaryStoragePoolInventory();
        inv.uuid = vo.getUuid();
        inv.primaryStorageUuid = vo.getPrimaryStorageUuid();
        inv.poolName = vo.getPoolName();
        inv.description = vo.getDescription();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.aliasName = vo.getAliasName();
        inv.type = vo.getType();
        return inv;
    }

    public static List<SurfsPrimaryStoragePoolInventory> valueOf(Collection<SurfsPrimaryStoragePoolVO> vos) {
        List<SurfsPrimaryStoragePoolInventory> invs = new ArrayList<>();
        for (SurfsPrimaryStoragePoolVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }
}
