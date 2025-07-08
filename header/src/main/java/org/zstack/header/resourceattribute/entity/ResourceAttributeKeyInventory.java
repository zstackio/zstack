package org.zstack.header.resourceattribute.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.StringDSL;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ResourceAttributeKeyVO.class)
@PythonClassInventory
public class ResourceAttributeKeyInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ResourceAttributeKeyInventory valueOf(ResourceAttributeKeyVO vo) {
        ResourceAttributeKeyInventory inv = new ResourceAttributeKeyInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ResourceAttributeKeyInventory> valueOf(Collection<ResourceAttributeKeyVO> vos) {
        return CollectionUtils.transform(vos, ResourceAttributeKeyInventory::valueOf);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public static ResourceAttributeKeyInventory __example__() {
        ResourceAttributeKeyInventory inventory = new ResourceAttributeKeyInventory();
        inventory.setUuid(StringDSL.createFixedUuid(ResourceAttributeKeyVO.class));
        inventory.setName("OperationsPersonnel");
        inventory.setDescription("Kinny");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return inventory;
    }
}
