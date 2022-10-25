//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DevicePlatformListType extends ApiPropertyBase {
    List<String> platform_name;
    public DevicePlatformListType() {
    }
    public DevicePlatformListType(List<String> platform_name) {
        this.platform_name = platform_name;
    }
    
    public List<String> getPlatformName() {
        return platform_name;
    }
    
    
    public void addPlatformName(String obj) {
        if (platform_name == null) {
            platform_name = new ArrayList<String>();
        }
        platform_name.add(obj);
    }
    public void clearPlatformName() {
        platform_name = null;
    }
    
}
