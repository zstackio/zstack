package org.zstack.header.keyprovider;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.Completion;

/**
 * Unified resource key management service.
 * Business layers (TPM, volume encryption, etc.) call this to create/retrieve DEKs
 * without knowing gRPC/key-tools protocol details.
 */
public interface EncryptedResourceKeyManager {

    /**
     * Get or create a resource encryption key.
     * <p>
     * Semantically reuses the existing key record for the same {@code (resourceType, resourceUuid)}
     * when one is already available; otherwise creates a new one and returns the plaintext DEK.
     * <p>
     * This contract does not guarantee concurrent linearizability by itself. Callers must not assume
     * the interface alone provides serialization, uniqueness enforcement, or transaction-level protection
     * across concurrent create requests.
     *
     * @param ctx        context describing the resource and key provider
     * @param completion returns {@link ResourceKeyResult} containing the plaintext DEK (base64)
     */
    void getOrCreateKey(GetOrCreateResourceKeyContext ctx,
                        ReturnValueCompletion<ResourceKeyResult> completion);

    /**
     * Roll back a newly created resource key during upper-layer workflow rollback.
     * <p>
     * If the key record already existed before creation, implementation should restore it
     * to its previous empty-placeholder state instead of deleting the relationship.
     */
    void rollbackCreatedKey(ResourceKeyResult result, Completion completion);

    class GetOrCreateResourceKeyContext {
        private String resourceUuid;
        private String resourceType;
        private String keyProviderUuid;
        private String purpose;

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getKeyProviderUuid() {
            return keyProviderUuid;
        }

        public void setKeyProviderUuid(String keyProviderUuid) {
            this.keyProviderUuid = keyProviderUuid;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }
    }

    class ResourceKeyResult {
        private String resourceUuid;
        private String resourceType;
        private String keyProviderUuid;
        private String keyProviderName;
        private String dekBase64;
        private String secretRef;
        private boolean createdNewKey;
        private boolean refExistedBeforeCreate;

        public String getResourceUuid() {
            return resourceUuid;
        }

        public void setResourceUuid(String resourceUuid) {
            this.resourceUuid = resourceUuid;
        }

        public String getResourceType() {
            return resourceType;
        }

        public void setResourceType(String resourceType) {
            this.resourceType = resourceType;
        }

        public String getKeyProviderUuid() {
            return keyProviderUuid;
        }

        public void setKeyProviderUuid(String keyProviderUuid) {
            this.keyProviderUuid = keyProviderUuid;
        }

        public String getKeyProviderName() {
            return keyProviderName;
        }

        public void setKeyProviderName(String keyProviderName) {
            this.keyProviderName = keyProviderName;
        }

        public String getDekBase64() {
            return dekBase64;
        }

        public void setDekBase64(String dekBase64) {
            this.dekBase64 = dekBase64;
        }

        public String getSecretRef() {
            return secretRef;
        }

        public void setSecretRef(String secretRef) {
            this.secretRef = secretRef;
        }

        public boolean isCreatedNewKey() {
            return createdNewKey;
        }

        public void setCreatedNewKey(boolean createdNewKey) {
            this.createdNewKey = createdNewKey;
        }

        public boolean isRefExistedBeforeCreate() {
            return refExistedBeforeCreate;
        }

        public void setRefExistedBeforeCreate(boolean refExistedBeforeCreate) {
            this.refExistedBeforeCreate = refExistedBeforeCreate;
        }
    }
}
