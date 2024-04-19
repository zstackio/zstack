package org.zstack.header.vm;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = TemplateVmInstanceCacheVO.class)
public class TemplateVmInstanceCacheInventory {
    private long id;
    private String templateVmInstanceUuid;
    private String cacheVmInstanceUuid;

    public static TemplateVmInstanceCacheInventory valueOf(TemplateVmInstanceCacheVO vo) {
        TemplateVmInstanceCacheInventory inv = new TemplateVmInstanceCacheInventory();
        inv.setId(vo.getId());
        inv.setTemplateVmInstanceUuid(vo.getTemplateVmInstanceUuid());
        inv.setCacheVmInstanceUuid(vo.getCacheVmInstanceUuid());
        return inv;
    }

    public static List<TemplateVmInstanceCacheInventory> valueOf(Collection<TemplateVmInstanceCacheVO> vos) {
        List<TemplateVmInstanceCacheInventory> invs = new ArrayList<TemplateVmInstanceCacheInventory>();
        for (TemplateVmInstanceCacheVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTemplateVmInstanceUuid() {
        return templateVmInstanceUuid;
    }

    public void setTemplateVmInstanceUuid(String templateVmInstanceUuid) {
        this.templateVmInstanceUuid = templateVmInstanceUuid;
    }

    public String getCacheVmInstanceUuid() {
        return cacheVmInstanceUuid;
    }

    public void setCacheVmInstanceUuid(String cacheVmInstanceUuid) {
        this.cacheVmInstanceUuid = cacheVmInstanceUuid;
    }
}
