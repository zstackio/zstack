package org.zstack.core.encrypt;

import org.zstack.header.exception.CloudRuntimeException;

public class DefaultEncryptDriver implements EncryptDriver {
    EncryptDriverType type = new EncryptDriverType(EncryptConstant.DEFAULT);

    public static EncryptRSA rsa = new EncryptRSA();

    @Override
    public EncryptDriverType getDriverType() {
        return type;
    }

    @Override
    public String encrypt(String data) {
        try {
            return rsa.encrypt1(data);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public String decrypt(String data) {
        try {
            return (String) rsa.decrypt1(data);
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public String encrypt(String data, String signed) {
        return rsa.encrypt(data, signed);
    }

    @Override
    public String decrypt(String data, String signed) {
        return rsa.decrypt(data, signed);
    }
}
