
package org.zstack.header.secret;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * Extension point for getting plaintext DEK from key-manager (e.g. premium with NKP/KMS).
 * Used for VM (e.g. to send DEK to host via SecretHostDefineMsg). Premium implements with
 * key-manager get; success returns dekBase64 (plaintext DEK, base64).
 */
public interface SecretGetExtensionPoint {
    /**
     * Get plaintext DEK. Implementation (e.g. premium) calls key-manager get.
     *
     * @param secretNameOrId secret name or id (from create or stored)
     * @param completion     success(dekBase64) with plaintext DEK in base64, or fail(error)
     */
    void getSecret(String secretNameOrId, ReturnValueCompletion<String> completion);
}
