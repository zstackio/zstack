
package org.zstack.header.secret;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * Extension point for creating a secret in key-manager (e.g. premium with NKP/KMS).
 * Used for VM (e.g. vTPM at VM create). Premium implements with key-manager create;
 * success returns secretId/name for later get.
 */
public interface SecretCreateExtensionPoint {
    /**
     * Create a secret. Implementation (e.g. premium) calls key-manager create.
     *
     * @param secretName name or identifier for the secret
     * @param completion success(secretIdOrName) for later get, or fail(error)
     */
    void createSecret(String secretName, ReturnValueCompletion<String> completion);
}
