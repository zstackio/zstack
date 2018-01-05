package org.zstack.storage.surfs.backup;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by zhouhaiping 2017-09-08.
 */
@Inventory(mappingVOClass = SurfsBackupStorageNodeVO.class)
public class SurfsBackupStorageNodeInventory {
    private String hostname;
    private Integer nodePort;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String backupStorageUuid;
    private String sshUsername;
    private String sshPassword;
    private Integer sshPort;
    private String status;
    private String nodeAddr;
    private String nodeUuid;

    public static SurfsBackupStorageNodeInventory valueOf(SurfsBackupStorageNodeVO vo) {
        SurfsBackupStorageNodeInventory inv = new SurfsBackupStorageNodeInventory();
        inv.setHostname(vo.getHostname());
        inv.setNodePort(vo.getNodePort());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        inv.setSshPort(vo.getSshPort());
        inv.setSshPassword(vo.getSshPassword());
        inv.setSshUsername(vo.getSshUsername());
        inv.setStatus(vo.getStatus().toString());
        inv.setNodeUuid(vo.getUuid());
        inv.setNodeAddr(vo.getHostname());
        return inv;
    }

    public static List<SurfsBackupStorageNodeInventory> valueOf(Collection<SurfsBackupStorageNodeVO> vos) {
        List<SurfsBackupStorageNodeInventory> invs = new ArrayList<SurfsBackupStorageNodeInventory>();
        for (SurfsBackupStorageNodeVO vo : vos) {
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

    public Integer getNodePort() {
        return nodePort;
    }

    public void setNodePort(Integer nodePort) {
        this.nodePort = nodePort;
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

    public String getNodeAddr() {
        return nodeAddr;
    }

    public void setNodeAddr(String nodeAddr) {
        this.nodeAddr = nodeAddr;
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }
}
