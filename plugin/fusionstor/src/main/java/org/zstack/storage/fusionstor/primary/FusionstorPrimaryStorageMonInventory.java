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
    private String monAddr;
    private String sshUsername;
    private String sshPassword;
    private Integer sshPort;
    private String status;
    private String monUuid;

    public static FusionstorPrimaryStorageMonInventory valueOf(FusionstorPrimaryStorageMonVO vo) {
        FusionstorPrimaryStorageMonInventory inv = new FusionstorPrimaryStorageMonInventory();
        inv.setHostname(vo.getHostname());
        inv.setMonPort(vo.getMonPort());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        inv.setSshPort(vo.getSshPort());
        inv.setSshUsername(vo.getSshUsername());
        inv.setSshPassword(vo.getSshPassword());
        inv.setStatus(vo.getStatus().toString());
        inv.setMonAddr(vo.getHostname());
        inv.setMonUuid(vo.getUuid());
        return inv;
    }

    public static List<FusionstorPrimaryStorageMonInventory> valueOf(Collection<FusionstorPrimaryStorageMonVO> vos) {
        List<FusionstorPrimaryStorageMonInventory> invs = new ArrayList<FusionstorPrimaryStorageMonInventory>();
        for (FusionstorPrimaryStorageMonVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
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

    public String getStatus() {
            return status;
    }

    public void setStatus(String status) {
            this.status = status;
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
