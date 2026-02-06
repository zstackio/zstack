package org.zstack.kvm.tpm;

import java.io.Serializable;

public class TpmTO implements Serializable {
    private String keyProviderUuid;

    public String getKeyProviderUuid() {
        return keyProviderUuid;
    }

    public void setKeyProviderUuid(String keyProviderUuid) {
        this.keyProviderUuid = keyProviderUuid;
    }
}
