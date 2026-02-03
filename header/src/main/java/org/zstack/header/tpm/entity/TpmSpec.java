package org.zstack.header.tpm.entity;

import org.zstack.utils.StringDSL;

public class TpmSpec {
    private boolean enable = true;
    private String keyProviderUuid;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getKeyProviderUuid() {
        return keyProviderUuid;
    }

    public void setKeyProviderUuid(String keyProviderUuid) {
        this.keyProviderUuid = keyProviderUuid;
    }

    public static TpmSpec __example__() {
        TpmSpec tpm = new TpmSpec();
        tpm.setKeyProviderUuid(StringDSL.createFixedUuid("keyProviderUuid"));
        return tpm;
    }
}
