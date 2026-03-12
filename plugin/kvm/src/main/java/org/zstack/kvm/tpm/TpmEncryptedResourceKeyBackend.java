package org.zstack.kvm.tpm;

import org.zstack.header.core.Completion;

/**
 * Responsible for handling the replication or reset of encryption resource keys
 * and other tasks in VM TPM cloning scenarios.
 */
public interface TpmEncryptedResourceKeyBackend {
    static class CloneEncryptedResourceKeyContext {
        public String srcTpmUuid;
        public String dstTpmUuid;

        /**
         * Whether to reset (regenerate) the key on the target TPM.
         * <ul>
         *   <li>{@code true}：Regenerate the key for the target TPM
         *   without inheriting the encrypted data from the source TPM.</li>
         *   <li>{@code false}：Copy the existing keys from the source TPM
         *   to the target TPM to ensure they remain consistent.</li>
         * </ul>
         */
        public boolean resetTpm;
    }

    /**
     * In a VM cloning scenario, copy or reset the encryption resource key
     * from the source TPM to the target TPM.
     */
    void cloneEncryptedResourceKey(CloneEncryptedResourceKeyContext context, Completion completion);
}
