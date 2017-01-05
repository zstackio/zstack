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
    private String sshUsername;
    private String sshPassword;
    private Integer sshPort;
    private String status;
    private String monAddr;
    private String monUuid;

    public static FusionstorBackupStorageMonInventory valueOf(FusionstorBackupStorageMonVO vo) {
        FusionstorBackupStorageMonInventory inv = new FusionstorBackupStorageMonInventory();
        inv.setHostname(vo.getHostname());
        inv.setMonPort(vo.getMonPort());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        inv.setSshPort(vo.getSshPort());
        inv.setSshPassword(vo.getSshPassword());
        inv.setSshUsername(vo.getSshUsername());
        inv.setStatus(vo.getStatus().toString());
        inv.setMonUuid(vo.getUuid());
        inv.setMonAddr(vo.getHostname());
        return inv;
    }

    public static List<FusionstorBackupStorageMonInventory> valueOf(Collection<FusionstorBackupStorageMonVO> vos) {
        List<FusionstorBackupStorageMonInventory> invs = new ArrayList<FusionstorBackupStorageMonInventory>();
        for (FusionstorBackupStorageMonVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getSshPassword() {

        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }
    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }
    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
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

    public String getStatus() {
            return status;
    }

    public void setStatus(String status) {
            this.status = status;
    }

    public String getMonAddr() {
        return monAddr;
    }

    public void setMonAddr(String monAddr) {
        this.monAddr = monAddr;
    }

    public String getMonUuid() {
        return monUuid;
    }

    public void setMonUuid(String monUuid) {
        this.monUuid = monUuid;
    }
}
