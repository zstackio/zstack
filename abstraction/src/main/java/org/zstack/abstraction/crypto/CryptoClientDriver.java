package org.zstack.abstraction.crypto;

import org.zstack.abstraction.PluginDriver;

import java.security.cert.X509Certificate;
import java.util.Map;

public interface CryptoClientDriver extends PluginDriver {
    boolean initialize(Map<String, String> properties);
    /**
     * Test client connectivity
     *
     * @return true means test connection succeed else false
     */
    boolean connect(Map<String, String> properties);

    /**
     * Parse certificate
     *
     * @param cert Certificate byte[]
     * @return X509Certificate instance
     */
    X509Certificate genericCertificate(byte[] cert);

    /**
     * Return SM3 encrypted cipher text
     *
     * @param plain the original text
     * @return encrypt result
     */
    String sm3Encrypt(String plain);

    /**
     * SM4 encryption of the string
     *
     * @param plain the original text
     * @return SM4 encrypted text
     */
    String sm4Encrypt(String plain);

    /**
     * SM4 decryption of the string
     *
     * @param plain the encrypted text
     * @return sSM4 decrypted text
     */
    String sm4Decrypt(String plain);

    /**
     * Encrypt the string with hmac
     *
     * @param plain the original text
     * @return hmac encrypted text
     */
    String hmac(String plain);
}
