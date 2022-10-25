//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class SequenceType extends ApiPropertyBase {
    Integer major;
    Integer minor;
    public SequenceType() {
    }
    public SequenceType(Integer major, Integer minor) {
        this.major = major;
        this.minor = minor;
    }
    public SequenceType(Integer major) {
        this(major, null);    }
    
    public Integer getMajor() {
        return major;
    }
    
    public void setMajor(Integer major) {
        this.major = major;
    }
    
    
    public Integer getMinor() {
        return minor;
    }
    
    public void setMinor(Integer minor) {
        this.minor = minor;
    }
    
}
