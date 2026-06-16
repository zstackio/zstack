package org.zstack.header.storage.addon.primary;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ExternalPrimaryStorageHostProtocolRefVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "externalPrimaryStorage", inventoryClass = ExternalPrimaryStorageInventory.class,
                foreignKey = "primaryStorageUuid", expandedInventoryKey = "uuid")
})
public class ExternalPrimaryStorageHostProtocolRefInventory implements Serializable {
    private String hostUuid;

    private String primaryStorageUuid;

    private String protocol;

    private String status;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public ExternalPrimaryStorageHostProtocolRefInventory() {
    }

    public ExternalPrimaryStorageHostProtocolRefInventory(ExternalPrimaryStorageHostProtocolRefVO vo) {
        this.hostUuid = vo.getHostUuid();
        this.primaryStorageUuid = vo.getPrimaryStorageUuid();
        this.protocol = vo.getProtocol();
        this.status = vo.getStatus() == null ? null : vo.getStatus().toString();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static ExternalPrimaryStorageHostProtocolRefInventory valueOf(ExternalPrimaryStorageHostProtocolRefVO vo) {
        return new ExternalPrimaryStorageHostProtocolRefInventory(vo);
    }

    public static List<ExternalPrimaryStorageHostProtocolRefInventory> valueOf1(Collection<ExternalPrimaryStorageHostProtocolRefVO> vos) {
        List<ExternalPrimaryStorageHostProtocolRefInventory> invs = new ArrayList<>();
        for (ExternalPrimaryStorageHostProtocolRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
