package org.zstack.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by lining on 2019/9/4.
 */
public class SHAUtils {
    public static String encrypt(String input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            md.update(input.getBytes("utf8"));
            BigInteger bigInteger = new BigInteger(1, md.digest());
            return String.format("%0128x", bigInteger);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDigestOfFile(String filePath, String algorithm, int precision) {
        try {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            md.update(fileData);
            BigInteger bigInteger = new BigInteger(1, md.digest());
            return String.format("%" + precision + "x", bigInteger);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
