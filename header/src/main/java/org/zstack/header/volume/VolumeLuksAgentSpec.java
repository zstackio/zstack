package org.zstack.header.volume;

import java.io.Serializable;

public class VolumeLuksAgentSpec implements Serializable {
    private static final long serialVersionUID = 1L;

    private String encryptSecretUuid;
    private String encryptLuksSecretMaterialFilePath;

    public boolean isComplete() {
        return (encryptSecretUuid != null && !encryptSecretUuid.isEmpty())
                || (encryptLuksSecretMaterialFilePath != null && !encryptLuksSecretMaterialFilePath.isEmpty());
    }

    public String getEncryptSecretUuid() {
        return encryptSecretUuid;
    }

    public void setEncryptSecretUuid(String encryptSecretUuid) {
        this.encryptSecretUuid = encryptSecretUuid;
    }

    public String getEncryptLuksSecretMaterialFilePath() {
        return encryptLuksSecretMaterialFilePath;
    }

    public void setEncryptLuksSecretMaterialFilePath(String encryptLuksSecretMaterialFilePath) {
        this.encryptLuksSecretMaterialFilePath = encryptLuksSecretMaterialFilePath;
    }
}
