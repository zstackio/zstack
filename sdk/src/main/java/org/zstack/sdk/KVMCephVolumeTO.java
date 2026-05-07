package org.zstack.sdk;



public class KVMCephVolumeTO extends org.zstack.sdk.VolumeTO {

    public java.util.List monInfo;
    public void setMonInfo(java.util.List monInfo) {
        this.monInfo = monInfo;
    }
    public java.util.List getMonInfo() {
        return this.monInfo;
    }

    public java.lang.String secretUuid;
    public void setSecretUuid(java.lang.String secretUuid) {
        this.secretUuid = secretUuid;
    }
    public java.lang.String getSecretUuid() {
        return this.secretUuid;
    }

    public java.lang.String encryptSecretUuid;
    public void setEncryptSecretUuid(java.lang.String encryptSecretUuid) {
        this.encryptSecretUuid = encryptSecretUuid;
    }
    public java.lang.String getEncryptSecretUuid() {
        return this.encryptSecretUuid;
    }

    public java.lang.String encryptFormat;
    public void setEncryptFormat(java.lang.String encryptFormat) {
        this.encryptFormat = encryptFormat;
    }
    public java.lang.String getEncryptFormat() {
        return this.encryptFormat;
    }

}
