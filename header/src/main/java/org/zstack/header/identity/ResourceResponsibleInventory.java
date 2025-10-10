package org.zstack.header.identity;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ResourceResponsibleVO.class)
public class ResourceResponsibleInventory {
    private String uuid;
    private String resourceUuid;
    private String responsibleType;
    private String responsibleUuid;
    private Timestamp lastOpDate;
    private Timestamp createDate;

    public static ResourceResponsibleInventory valueOf(ResourceResponsibleVO vo) {
        ResourceResponsibleInventory inv = new ResourceResponsibleInventory();
        inv.setUuid(vo.getUuid());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setResponsibleType(vo.getResponsibleType());
        inv.setResponsibleUuid(vo.getResponsibleUuid());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<ResourceResponsibleInventory> valueOf(Collection<ResourceResponsibleVO> vos) {
        List<ResourceResponsibleInventory> invs = new ArrayList<>();
        for (ResourceResponsibleVO vo : vos) {
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

    public String getResponsibleType() {
        return responsibleType;
    }

    public void setResponsibleType(String responsibleType) {
        this.responsibleType = responsibleType;
    }

    public String getResponsibleUuid() {
        return responsibleUuid;
    }

    public void setResponsibleUuid(String responsibleUuid) {
        this.responsibleUuid = responsibleUuid;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
