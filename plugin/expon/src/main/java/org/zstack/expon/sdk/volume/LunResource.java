package org.zstack.expon.sdk.volume;

import org.zstack.expon.sdk.Param;

public class LunResource {
    @Param
    private String lunId;

    @Param(validValues = {"volume", "snapshot"})
    private String lunType;

    @Param(required = false)
    private String isReadonly;

    public String getLunId() {
        return lunId;
    }

    public void setLunId(String lunId) {
        this.lunId = lunId;
    }

    public String getLunType() {
        return lunType;
    }

    public void setLunType(String lunType) {
        this.lunType = lunType;
    }

    public void setIsReadonly(String isReadonly) {
        this.isReadonly = isReadonly;
    }

    public String getIsReadonly() {
        return isReadonly;
    }

    public LunResource() {
    }

    public LunResource(String lunId, String lunType) {
        this.lunId = lunId;
        this.lunType = lunType;
    }
}
