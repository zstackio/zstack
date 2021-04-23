package org.zstack.utils;

import com.sansec.devicev4.util.BytesUtil;
import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;


/**
 * Created by Jialong.Dong on 2021/04/16.
 */
public class DigestUtils {
    private String algorithm;

    private String provider;

    public DigestUtils(String algorithm) {
        this.algorithm = algorithm;
    }

    public DigestUtils(String algorithm, String provider) {
        this.algorithm = algorithm;
        this.provider = provider;
    }

    public String encrypt(String input) {
        try {
            MessageDigest md;
            if (provider != null) {
                md = MessageDigest.getInstance(algorithm, provider);
            } else {
                md = MessageDigest.getInstance(algorithm);
            }
            md.reset();
            md.update(input.getBytes("utf8"));
            byte[] hex = md.digest();
            return BytesUtil.bytes2hex(hex).split(",")[0];
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
