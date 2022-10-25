//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DnsSoaRecordType extends ApiPropertyBase {
    Integer negative_cache_ttl_seconds;
    public DnsSoaRecordType() {
    }
    public DnsSoaRecordType(Integer negative_cache_ttl_seconds) {
        this.negative_cache_ttl_seconds = negative_cache_ttl_seconds;
    }
    
    public Integer getNegativeCacheTtlSeconds() {
        return negative_cache_ttl_seconds;
    }
    
    public void setNegativeCacheTtlSeconds(Integer negative_cache_ttl_seconds) {
        this.negative_cache_ttl_seconds = negative_cache_ttl_seconds;
    }
    
}
