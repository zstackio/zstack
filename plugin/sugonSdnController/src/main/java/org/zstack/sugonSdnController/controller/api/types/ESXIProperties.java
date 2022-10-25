//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ESXIProperties extends ApiPropertyBase {
    String dvs_name;
    String dvs_id;
    public ESXIProperties() {
    }
    public ESXIProperties(String dvs_name, String dvs_id) {
        this.dvs_name = dvs_name;
        this.dvs_id = dvs_id;
    }
    public ESXIProperties(String dvs_name) {
        this(dvs_name, null);    }
    
    public String getDvsName() {
        return dvs_name;
    }
    
    public void setDvsName(String dvs_name) {
        this.dvs_name = dvs_name;
    }
    
    
    public String getDvsId() {
        return dvs_id;
    }
    
    public void setDvsId(String dvs_id) {
        this.dvs_id = dvs_id;
    }
    
}
