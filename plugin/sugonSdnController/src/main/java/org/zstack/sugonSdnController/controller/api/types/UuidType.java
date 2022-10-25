//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class UuidType extends ApiPropertyBase {
    Long uuid_mslong;
    Long uuid_lslong;
    public UuidType() {
    }
    public UuidType(Long uuid_mslong, Long uuid_lslong) {
        this.uuid_mslong = uuid_mslong;
        this.uuid_lslong = uuid_lslong;
    }
    public UuidType(Long uuid_mslong) {
        this(uuid_mslong, null);    }
    
    public Long getUuidMslong() {
        return uuid_mslong;
    }
    
    public void setUuidMslong(Long uuid_mslong) {
        this.uuid_mslong = uuid_mslong;
    }
    
    
    public Long getUuidLslong() {
        return uuid_lslong;
    }
    
    public void setUuidLslong(Long uuid_lslong) {
        this.uuid_lslong = uuid_lslong;
    }
    
}
