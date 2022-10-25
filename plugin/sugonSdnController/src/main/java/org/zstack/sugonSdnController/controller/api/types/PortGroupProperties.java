//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PortGroupProperties extends ApiPropertyBase {
    Integer miimon;
    String xmit_hash_policy;
    public PortGroupProperties() {
    }
    public PortGroupProperties(Integer miimon, String xmit_hash_policy) {
        this.miimon = miimon;
        this.xmit_hash_policy = xmit_hash_policy;
    }
    public PortGroupProperties(Integer miimon) {
        this(miimon, null);    }
    
    public Integer getMiimon() {
        return miimon;
    }
    
    public void setMiimon(Integer miimon) {
        this.miimon = miimon;
    }
    
    
    public String getXmitHashPolicy() {
        return xmit_hash_policy;
    }
    
    public void setXmitHashPolicy(String xmit_hash_policy) {
        this.xmit_hash_policy = xmit_hash_policy;
    }
    
}
