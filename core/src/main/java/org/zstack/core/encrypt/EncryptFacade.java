package org.zstack.core.encrypt;

import java.security.cert.X509Certificate;

/**
 * Created by kayo on 2018/9/7.
 */
public interface EncryptFacade {
    String encrypt(String decryptString);

    String decrypt(String encryptString);
    /**
     * Decrypt cipher text to plain text, with certificate info.
     *
     * @return Not null.
     * If decrypt process raise an Exception, describe it in {@link EncryptFacadeResult#error}.
     */
    EncryptFacadeResult<String> attachedVerify(String cipherText);

    /**
     * Parse the CCS certificate cipher text and return the CCS certificate information.
     * @return CCS certificate information, not null
     */
    EncryptFacadeResult<X509Certificate> parseCertificate(String certificateText);

    EncryptFacadeResult<String> encrypt(String data, EncryptType algType);

    EncryptFacadeResult<String> decrypt(String data, EncryptType algType);
}