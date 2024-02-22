package org.zstack.header.vo;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by xing5 on 2017/5/1.
 */
@Inventory(mappingVOClass = ResourceVO.class)
public class ResourceInventory {
    protected String uuid;
    private String resourceName;
    private String resourceType;
    private String concreteResourceType;

    public static ResourceInventory valueOf(ResourceVO vo) {
        ResourceInventory inv = new ResourceInventory();
        inv.setResourceName(vo.getResourceName());
        inv.setResourceType(vo.getResourceType());
        inv.setConcreteResourceType(vo.getConcreteResourceType());
        inv.setUuid(vo.getUuid());
        return inv;
    }

    public static List<ResourceInventory> valueOf(Collection<ResourceVO> vos) {
        List<ResourceInventory> invs = new ArrayList<>();
        for (ResourceVO vo : vos) {
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

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getConcreteResourceType() {
        return concreteResourceType;
    }

    public void setConcreteResourceType(String concreteResourceType) {
        this.concreteResourceType = concreteResourceType;
    }
}
