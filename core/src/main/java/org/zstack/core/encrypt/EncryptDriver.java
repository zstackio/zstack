package org.zstack.core.encrypt;

import org.zstack.header.errorcode.ErrorableValue;
import java.util.List;

public interface EncryptDriver {
    String encryptError = "%s encrypt failed";
    String decryptError = "%s decrypt failed";

    EncryptDriverType getDriverType();

    default List<String> getDriverTypes() {
        return null;
    }


    String encrypt(String data);

    String decrypt(String data);

    ErrorableValue<String> encrypt(String data, String algType);

    ErrorableValue<String> decrypt(String data, String algType);
}
