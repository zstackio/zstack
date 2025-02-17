package org.zstack.abstraction.crypto;

import org.zstack.abstraction.OptionType;
import org.zstack.abstraction.PluginDriver;

import java.util.Collection;
import java.util.HashMap;
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
     * Parse original text and certificate base64 string
     *
     * @param input Signature string representation
     * @return A 2D byte array where Result[0] contains the original text and Result[1] contains the certificate base64 string
     */
    byte[][] attachedVerify(String input);

    /**
     * Retrieves additional signature properties.
     *
     * @return A map containing additional signature properties,
     * where each key-value pair represents a signature-related configuration or metadata.
     */
    default Map<String, String> additionSignatureProperties(){
        return new HashMap<>();
    };

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

    Collection<OptionType> optionTypes();
}
