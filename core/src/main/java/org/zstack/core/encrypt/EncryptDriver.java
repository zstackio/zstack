package org.zstack.core.encrypt;

public interface EncryptDriver {
    EncryptDriverType getDriverType();

    String encrypt(String data);

    String decrypt(String data);
}
