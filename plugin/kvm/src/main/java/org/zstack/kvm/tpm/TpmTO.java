package org.zstack.kvm.tpm;

import java.io.Serializable;

public class TpmTO implements Serializable {
    private String keyProviderUuid;
    private String installPath;

    public String getKeyProviderUuid() {
        return keyProviderUuid;
    }

    public void setKeyProviderUuid(String keyProviderUuid) {
        this.keyProviderUuid = keyProviderUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
