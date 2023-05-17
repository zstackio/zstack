package org.zstack.core.encrypt;

import org.zstack.header.core.encrypt.EncryptEntityState;
import org.zstack.header.core.encrypt.EncryptedFieldBundle;

import java.util.List;

/**
 * Created by kayo on 2018/9/7.
 */
public interface EncryptFacade {
    String encrypt(String decryptString);

    String decrypt(String encryptString);

    EncryptFacadeResult<String> encrypt(String data, String algType);

    EncryptFacadeResult<String> decrypt(String data, String algType);

    void updateEncryptDataStateIfExists(String entity, String column, EncryptEntityState state);

    List<EncryptedFieldBundle> getIntegrityEncryptionBundle();

    List<EncryptedFieldBundle> getConfidentialityEncryptionBundle();
}