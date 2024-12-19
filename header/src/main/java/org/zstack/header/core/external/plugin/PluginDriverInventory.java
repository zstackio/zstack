package org.zstack.header.core.external.plugin;

import org.zstack.abstraction.OptionType;
import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Inventory(mappingVOClass = PluginDriverVO.class)
public class PluginDriverInventory {
    private String uuid;
    private String name;
    private String type;
    private String vendor;
    private String features;
    private Collection<OptionType> optionTypes;
    private String license;
    private String version;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PluginDriverInventory valueOf(PluginDriverVO vo) {
        PluginDriverInventory inv = new PluginDriverInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setVendor(vo.getVendor());
        inv.setFeatures(vo.getFeatures());
        inv.setType(vo.getType());
        inv.setLicense(vo.getLicense());
        inv.setVersion(vo.getVersion());
        inv.setDescription(vo.getDescription());
        inv.setOptionTypes(JSONObjectUtil.toCollection(vo.getOptionTypes(), ArrayList.class, OptionType.class));
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<PluginDriverInventory> valueOf(Collection<PluginDriverVO> vos) {
        List<PluginDriverInventory> invs = new ArrayList<>();
        for (PluginDriverVO vo : vos) {
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

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Collection<OptionType> getOptionTypes() {
        return optionTypes;
    }

    public void setOptionTypes(Collection<OptionType> optionTypes) {
        this.optionTypes = optionTypes;
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
