package org.zstack.test.integration.kvm.host

import org.zstack.header.secret.HostSecretEnvelopeCryptoExtensionPoint

/**
 * Mock implementation for integration test when premium/crypto is not on classpath.
 * Returns a fake envelope (32-byte enc + plaintext + 12 tag) so KVM proceeds to call the agent simulator.
 */
class MockHostSecretEnvelopeCryptoExtensionPoint implements HostSecretEnvelopeCryptoExtensionPoint {
    @Override
    byte[] seal(byte[] recipientPublicKey, byte[] plaintext) throws Exception {
        if (recipientPublicKey == null || recipientPublicKey.length != 32 || plaintext == null) {
            throw new IllegalArgumentException("recipientPublicKey must be 32 bytes, plaintext non-null")
        }
        int encLen = 32
        int tagLen = 12
        byte[] envelope = new byte[encLen + plaintext.length + tagLen]
        System.arraycopy(recipientPublicKey, 0, envelope, 0, encLen)
        System.arraycopy(plaintext, 0, envelope, encLen, plaintext.length)
        return envelope
    }
}
