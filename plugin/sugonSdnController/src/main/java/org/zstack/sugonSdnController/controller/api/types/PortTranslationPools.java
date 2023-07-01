//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PortTranslationPools extends ApiPropertyBase {
    List<PortTranslationPool> port_translation_pool;
    public PortTranslationPools() {
    }
    public PortTranslationPools(List<PortTranslationPool> port_translation_pool) {
        this.port_translation_pool = port_translation_pool;
    }
    
    public List<PortTranslationPool> getPortTranslationPool() {
        return port_translation_pool;
    }
    
    
    public void addPortTranslationPool(PortTranslationPool obj) {
        if (port_translation_pool == null) {
            port_translation_pool = new ArrayList<PortTranslationPool>();
        }
        port_translation_pool.add(obj);
    }
    public void clearPortTranslationPool() {
        port_translation_pool = null;
    }
    
    
    public void addPortTranslationPool(String protocol, PortType port_range, String port_count) {
        if (port_translation_pool == null) {
            port_translation_pool = new ArrayList<PortTranslationPool>();
        }
        port_translation_pool.add(new PortTranslationPool(protocol, port_range, port_count));
    }
    
}
