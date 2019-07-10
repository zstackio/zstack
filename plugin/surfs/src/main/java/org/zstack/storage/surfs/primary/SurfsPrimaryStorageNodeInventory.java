package org.zstack.storage.surfs.primary;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@Inventory(mappingVOClass = SurfsPrimaryStorageNodeVO.class)
public class SurfsPrimaryStorageNodeInventory {
    private String hostname;
    private Integer nodePort;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String primaryStorageUuid;
    private String nodeAddr;
    private String sshUsername;
    private String sshPassword;
    private Integer sshPort;
    private String status;
    private String nodeUuid;

    public static SurfsPrimaryStorageNodeInventory valueOf(SurfsPrimaryStorageNodeVO vo) {
        SurfsPrimaryStorageNodeInventory inv = new SurfsPrimaryStorageNodeInventory();
        inv.setHostname(vo.getHostname());
        inv.setNodePort(vo.getNodePort());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        inv.setSshPort(vo.getSshPort());
        inv.setSshUsername(vo.getSshUsername());
        inv.setSshPassword(vo.getSshPassword());
        inv.setStatus(vo.getStatus().toString());
        inv.setNodeAddr(vo.getHostname());
        inv.setNodeUuid(vo.getUuid());
        return inv;
    }

    public static List<SurfsPrimaryStorageNodeInventory> valueOf(Collection<SurfsPrimaryStorageNodeVO> vos) {
        List<SurfsPrimaryStorageNodeInventory> invs = new ArrayList<SurfsPrimaryStorageNodeInventory>();
        for (SurfsPrimaryStorageNodeVO vo : vos) {
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

    public Integer getNodePort() {
        return nodePort;
    }

    public void setNodePort(Integer monPort) {
        this.nodePort = monPort;
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
