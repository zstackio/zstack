//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class AsnRangeType extends ApiPropertyBase {
    Integer asn_min;
    Integer asn_max;
    public AsnRangeType() {
    }
    public AsnRangeType(Integer asn_min, Integer asn_max) {
        this.asn_min = asn_min;
        this.asn_max = asn_max;
    }
    public AsnRangeType(Integer asn_min) {
        this(asn_min, null);    }
    
    public Integer getAsnMin() {
        return asn_min;
    }
    
    public void setAsnMin(Integer asn_min) {
        this.asn_min = asn_min;
    }
    
    
    public Integer getAsnMax() {
        return asn_max;
    }
    
    public void setAsnMax(Integer asn_max) {
        this.asn_max = asn_max;
    }
    
}
