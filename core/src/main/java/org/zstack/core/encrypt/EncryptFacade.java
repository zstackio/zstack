package org.zstack.core.encrypt;

import org.zstack.header.core.encrypt.EncryptEntityState;
import org.zstack.header.errorcode.ErrorableValue;

/**
 * Created by kayo on 2018/9/7.
 */
public interface EncryptFacade {
    String encrypt(String decryptString);

    String decrypt(String encryptString);

    ErrorableValue<String> encrypt(String data, String algType);

    ErrorableValue<String> decrypt(String data, String algType);

    void updateEncryptDataStateIfExists(String entity, String column, EncryptEntityState state);
}