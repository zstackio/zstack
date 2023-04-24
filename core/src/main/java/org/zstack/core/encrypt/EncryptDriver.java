package org.zstack.core.encrypt;

public interface EncryptDriver {
    String encryptError = "%s encrypt failed";
    String decryptError = "%s decrypt failed";

    EncryptDriverType getDriverType();

    String encrypt(String data);

    String decrypt(String data);

    EncryptFacadeResult<String> encrypt(String data, String algType);

    EncryptFacadeResult<String> decrypt(String data, String algType);
}
