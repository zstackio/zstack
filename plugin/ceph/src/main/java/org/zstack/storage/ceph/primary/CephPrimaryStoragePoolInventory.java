package org.zstack.storage.ceph.primary;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/2/28.
 */
@Inventory(mappingVOClass = CephPrimaryStoragePoolVO.class)
public class CephPrimaryStoragePoolInventory {
    private String uuid;
    private String primaryStorageUuid;
    private String poolName;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static CephPrimaryStoragePoolInventory valueOf(CephPrimaryStoragePoolVO vo) {
        CephPrimaryStoragePoolInventory inv = new CephPrimaryStoragePoolInventory();
        inv.uuid = vo.getUuid();
        inv.primaryStorageUuid = vo.getPrimaryStorageUuid();
        inv.poolName = vo.getPoolName();
        inv.description = vo.getDescription();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<CephPrimaryStoragePoolInventory> valueOf(Collection<CephPrimaryStoragePoolVO> vos) {
        List<CephPrimaryStoragePoolInventory> invs = new ArrayList<>();
        for (CephPrimaryStoragePoolVO vo : vos) {
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
}
