package org.zstack.storage.fusionstor.backup;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/27/2015.
 */
@Inventory(mappingVOClass = FusionstorBackupStorageMonVO.class)
public class FusionstorBackupStorageMonInventory {
    private String hostname;
    private Integer monPort;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String backupStorageUuid;

    public static FusionstorBackupStorageMonInventory valueOf(FusionstorBackupStorageMonVO vo) {
        FusionstorBackupStorageMonInventory inv = new FusionstorBackupStorageMonInventory();
        inv.setHostname(vo.getHostname());
        inv.setMonPort(vo.getMonPort());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        return inv;
    }

    public static List<FusionstorBackupStorageMonInventory> valueOf(Collection<FusionstorBackupStorageMonVO> vos) {
        List<FusionstorBackupStorageMonInventory> invs = new ArrayList<FusionstorBackupStorageMonInventory>();
        for (FusionstorBackupStorageMonVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public Integer getMonPort() {
        return monPort;
    }

    public void setMonPort(Integer monPort) {
        this.monPort = monPort;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
