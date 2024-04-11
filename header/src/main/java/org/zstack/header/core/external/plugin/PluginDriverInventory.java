package org.zstack.header.core.external.plugin;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = PluginDriverVO.class)
public class PluginDriverInventory {
    private String name;
    private String type;
    private String vendor;
    private String features;

    public static PluginDriverInventory valueOf(PluginDriverVO vo) {
        PluginDriverInventory inv = new PluginDriverInventory();
        inv.setName(vo.getName());
        inv.setVendor(vo.getVendor());
        inv.setFeatures(vo.getFeatures());
        inv.setType(vo.getType());
        return inv;
    }

    public static List<PluginDriverInventory> valueOf(Collection<PluginDriverVO> vos) {
        List<PluginDriverInventory> invs = new ArrayList<>();
        for (PluginDriverVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getFeatures() {
        return features;
    }

    public void setFeatures(String features) {
        this.features = features;
    }
}
