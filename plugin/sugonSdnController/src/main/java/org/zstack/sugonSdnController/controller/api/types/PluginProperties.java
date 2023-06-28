//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PluginProperties extends ApiPropertyBase {
    List<PluginProperty> plugin_property;
    public PluginProperties() {
    }
    public PluginProperties(List<PluginProperty> plugin_property) {
        this.plugin_property = plugin_property;
    }
    
    public List<PluginProperty> getPluginProperty() {
        return plugin_property;
    }
    
    
    public void addPluginProperty(PluginProperty obj) {
        if (plugin_property == null) {
            plugin_property = new ArrayList<PluginProperty>();
        }
        plugin_property.add(obj);
    }
    public void clearPluginProperty() {
        plugin_property = null;
    }
    
    
    public void addPluginProperty(String property, String value) {
        if (plugin_property == null) {
            plugin_property = new ArrayList<PluginProperty>();
        }
        plugin_property.add(new PluginProperty(property, value));
    }
    
}
