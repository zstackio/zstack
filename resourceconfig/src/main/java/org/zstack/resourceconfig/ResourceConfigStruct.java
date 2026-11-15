package org.zstack.resourceconfig;

import java.util.List;
import java.util.Map;

public class ResourceConfigStruct {
    private Map<String, Object> runtimeState;
    public Map<String, Object> getRuntimeState() { return runtimeState; }
    public void setRuntimeState(Map<String, Object> value) { runtimeState = value; }

    private String value;
    private List<ResourceConfigInventory> effectiveConfigs;
    private String name;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<ResourceConfigInventory> getEffectiveConfigs() {
        return effectiveConfigs;
    }

    public void setEffectiveConfigs(List<ResourceConfigInventory> effectiveConfigs) {
        this.effectiveConfigs = effectiveConfigs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
