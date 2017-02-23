package org.zstack.storage.fusionstor.primary;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@Inventory(mappingVOClass = FusionstorPrimaryStorageMonVO.class)
public class FusionstorPrimaryStorageMonInventory {
    private String hostname;
    private Integer monPort;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String primaryStorageUuid;

    public static FusionstorPrimaryStorageMonInventory valueOf(FusionstorPrimaryStorageMonVO vo) {
        FusionstorPrimaryStorageMonInventory inv = new FusionstorPrimaryStorageMonInventory();
        inv.setHostname(vo.getHostname());
        inv.setMonPort(vo.getMonPort());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        return inv;
    }

    public static List<FusionstorPrimaryStorageMonInventory> valueOf(Collection<FusionstorPrimaryStorageMonVO> vos) {
        List<FusionstorPrimaryStorageMonInventory> invs = new ArrayList<FusionstorPrimaryStorageMonInventory>();
        for (FusionstorPrimaryStorageMonVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public Integer getMonPort() {
        return monPort;
    }

    public void setMonPort(Integer monPort) {
        this.monPort = monPort;
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
