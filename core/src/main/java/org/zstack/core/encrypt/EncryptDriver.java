package org.zstack.core.encrypt;

import org.zstack.header.errorcode.ErrorableValue;

public interface EncryptDriver {
    EncryptDriverType getDriverType();

    String encrypt(String data);

    String decrypt(String data);

    ErrorableValue<String> encrypt(String data, String algType);

    ErrorableValue<String> decrypt(String data, String algType);
}
