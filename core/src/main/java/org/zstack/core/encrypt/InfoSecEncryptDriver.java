package org.zstack.core.encrypt;

/**
 * @Author: DaoDao
 * @Date: 2021/10/28
 */
public class InfoSecEncryptDriver implements EncryptDriver {
    EncryptDriverType type = new EncryptDriverType(EncryptConstant.INFOSEC);

    @Override
    public EncryptDriverType getDriverType() {
        return type;
    }

    @Override
    public String encrypt(String data) {
        return null;
    }

    @Override
    public String decrypt(String data) {
        return null;
    }

    @Override
    public String encrypt(String data, String signed) {
        return null;
    }

    @Override
    public String decrypt(String data, String signed) {
        return null;
    }
}
