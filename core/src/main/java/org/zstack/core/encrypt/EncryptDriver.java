package org.zstack.core.encrypt;

import java.security.cert.X509Certificate;

public interface EncryptDriver {
    EncryptDriverType getDriverType();

    String encrypt(String data);

    String decrypt(String data);
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

    EncryptFacadeResult<String> encrypt(String data, String algType);

    EncryptFacadeResult<String> decrypt(String data, String algType);
}
