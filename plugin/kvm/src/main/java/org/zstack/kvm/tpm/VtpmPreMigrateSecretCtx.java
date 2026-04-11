package org.zstack.kvm.tpm;

import org.zstack.header.keyprovider.EncryptedResourceKeyManager.ResourceKeyResult;

import java.util.Map;

/**
 * Context stored in migrate flow chain {@code Map data} for vTPM secret handling
 * before {@code MigrateVmOnHypervisorMsg} / libvirt migration.
 */
public final class VtpmPreMigrateSecretCtx {

    public static final String DATA_MAP_KEY = "vtpmPreMigrateSecretCtx";

    private boolean skipAll;
    private String tpmUuid;
    private String providerUuid;
    private String providerName;
    private Integer keyVersion;
    private ResourceKeyResult resourceKeyResult;
    private String sourceSecretUuid;

    public boolean isSkipAll() {
        return skipAll;
    }

    public void setSkipAll(boolean skipAll) {
        this.skipAll = skipAll;
    }

    public String getTpmUuid() {
        return tpmUuid;
    }

    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    public String getProviderUuid() {
        return providerUuid;
    }

    public void setProviderUuid(String providerUuid) {
        this.providerUuid = providerUuid;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }

    public ResourceKeyResult getResourceKeyResult() {
        return resourceKeyResult;
    }

    public void setResourceKeyResult(ResourceKeyResult resourceKeyResult) {
        this.resourceKeyResult = resourceKeyResult;
    }

    public String getSourceSecretUuid() {
        return sourceSecretUuid;
    }

    public void setSourceSecretUuid(String sourceSecretUuid) {
        this.sourceSecretUuid = sourceSecretUuid;
    }

    public static VtpmPreMigrateSecretCtx from(Map data) {
        Object o = data.get(DATA_MAP_KEY);
        return o instanceof VtpmPreMigrateSecretCtx ? (VtpmPreMigrateSecretCtx) o : null;
    }
}
