package org.zstack.core.encrypt;

/**
 * Created by kayo on 2018/9/7.
 */
public interface EncryptFacade {
    String encrypt(String decryptString);

    String decrypt(String encryptString);

    EncryptFacadeResult<String> encrypt(String data, String algType);

    EncryptFacadeResult<String> decrypt(String data, String algType);
}