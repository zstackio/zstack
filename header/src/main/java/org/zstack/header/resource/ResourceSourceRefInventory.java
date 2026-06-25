package org.zstack.header.resource;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ResourceSourceRefVO.class)
public class ResourceSourceRefInventory {
    private String uuid;
    private String resourceUuid;
    private String resourceType;
    private String sourceType;
    private String sourceName;
    private String externalUuid;
    private String externalType;
    private String syncType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ResourceSourceRefInventory valueOf(ResourceSourceRefVO vo) {
        ResourceSourceRefInventory inv = new ResourceSourceRefInventory();
        inv.uuid = vo.getUuid();
        inv.resourceUuid = vo.getResourceUuid();
        inv.resourceType = vo.getResourceType();
        inv.sourceType = vo.getSourceType();
        inv.sourceName = vo.getSourceName();
        inv.externalUuid = vo.getExternalUuid();
        inv.externalType = vo.getExternalType();
        inv.syncType = vo.getSyncType();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<ResourceSourceRefInventory> valueOf(Collection<ResourceSourceRefVO> vos) {
        List<ResourceSourceRefInventory> invs = new ArrayList<>(vos.size());
        for (ResourceSourceRefVO vo : vos) {
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

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getExternalUuid() {
        return externalUuid;
    }

    public void setExternalUuid(String externalUuid) {
        this.externalUuid = externalUuid;
    }

    public String getExternalType() {
        return externalType;
    }

    public void setExternalType(String externalType) {
        this.externalType = externalType;
    }

    public String getSyncType() {
        return syncType;
    }

    public void setSyncType(String syncType) {
        this.syncType = syncType;
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
