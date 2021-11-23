package org.zstack.core.encrypt;

import org.zstack.header.core.encrypt.EncryptConstant;
import org.zstack.header.exception.CloudRuntimeException;

import java.security.cert.X509Certificate;

public class DefaultEncryptDriver implements EncryptDriver {
    private EncryptDriverType type = new EncryptDriverType(EncryptConstant.DEFAULT);

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
    public EncryptFacadeResult<String> attachedVerify(String cipherText) {
        return null;
    }

    @Override
    public EncryptFacadeResult<X509Certificate> parseCertificate(String certificateText) {
        return null;
    }

    @Override
    public EncryptFacadeResult<String> encrypt(String data, String algType) {
        try {
            return new EncryptFacadeResult<>(rsa.encrypt(data, algType));
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }

    @Override
    public EncryptFacadeResult<String> decrypt(String data, String algType) {
        try {
            return new EncryptFacadeResult<>(rsa.decrypt(data, algType));
        } catch (Exception e) {
            throw new CloudRuntimeException(e.getMessage());
        }
    }
}
