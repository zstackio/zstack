package org.zstack.test.integration.kvm.host.secret

import org.zstack.kvm.HostSecretEnvelopeCryptoExtensionPoint

/**
 * Test-side mock of HostSecretEnvelopeCryptoExtensionPoint.
 * Does NOT call premium crypto; just returns a fake envelope that looks structurally valid to the agent.
 */
class HostSecretEnvelopeCryptoTestExtension implements HostSecretEnvelopeCryptoExtensionPoint {
    @Override
    byte[] seal(byte[] recipientPublicKey, byte[] plaintext) throws Exception {
        if (recipientPublicKey == null || recipientPublicKey.length != 32 || plaintext == null) {
            throw new IllegalArgumentException("recipientPublicKey must be 32 bytes, plaintext non-null")
        }
        int encLen = 32
        int tagLen = 12
        byte[] envelope = new byte[encLen + plaintext.length + tagLen]
        // Put the recipient public key into the "enc" slot so envelope[0..31] looks like a plausible X25519 public key.
        System.arraycopy(recipientPublicKey, 0, envelope, 0, encLen)
        // Copy plaintext in the middle; the simulator never decrypts it.
        System.arraycopy(plaintext, 0, envelope, encLen, plaintext.length)
        // Leave the last 12 bytes as zeros to mimic an AEAD tag.
        return envelope
    }
}
