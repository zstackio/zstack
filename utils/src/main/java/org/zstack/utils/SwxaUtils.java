package org.zstack.utils;

import org.apache.commons.codec.binary.Base64;
import org.zstack.utils.logging.CLogger;

//import com.sansec.devicev4.util.BytesUtil;
//import com.sansec.jce.provider.SwxaProvider;

import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

/**
 * Created by Jialong.Dong on 2021/04/16.
 */

public class SwxaUtils {

    private static CLogger logger = Utils.getLogger(SwxaUtils.class);

//    public static SwxaProvider swxaProvider = new SwxaProvider();

    private static int keyIndex = 1;

    public static String[] digestModes = {"SM3","SM3WithoutId","SHA1","SHA224", "SHA256","SHA512","SHA3-256","SHA3-384","SHA3-512"};

    private static String alg = "SM4";

    private static String transformation = "SM4/ECB/PKCS5PADDING";

//    public static Key genInternalKey(String alg) throws Exception {
//        KeyGenerator keyGenerator = KeyGenerator.getInstance(alg, swxaProvider);
//        keyGenerator.init(keyIndex << 16);
//        Key key = keyGenerator.generateKey();
//        return key;
//    }
//
//    public static String encrypt(String data) {
//        try {
//            Cipher cipher = Cipher.getInstance(transformation, swxaProvider);
//            cipher.init(Cipher.ENCRYPT_MODE, genInternalKey(alg));
//
//            logger.info(String.format("encrypt data by %s mode.", transformation));
//            byte[] res = cipher.doFinal(data.getBytes("utf8"));
//            return BytesUtil.bytes2hex(res);
//        } catch (Exception e){
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static String decrypt(String data) {
//        try {
//            Cipher cipher = Cipher.getInstance(transformation, swxaProvider);
//            cipher.init(Cipher.DECRYPT_MODE, genInternalKey(alg));
//
//            logger.info(String.format("decrypt data by %s mode.", transformation));
//            byte[] res = cipher.doFinal(BytesUtil.hex2bytes(data));
//            return BytesUtil.bytes2hex(res);
//        } catch (Exception e){
//            throw new RuntimeException(e);
//        }
//    }
//
//    public static String getDigest(String input, String digestMode){
//        return new DigestUtils(digestMode, "SwxaJCE").encrypt(input);
//    }
//
//    // cipherData is encrypt by SM4 and Digest by SM3 of originData, format: xxx@yyy, where xxx=SM4(od) yyy=SM3(xxx)
//    public static String verifyAndDecryptCipherData(String cipherData) {
//        String encryptedData = cipherData.split("@")[0];
//        String hexData = cipherData.split("@")[1];
//        if (! hexData.equals(getDigest(encryptedData, "SM3"))) {
//            throw new RuntimeException(String.format("Failed to verify the input[%s]", cipherData));
//        }
//        return decrypt(encryptedData);
//    }
//
//    public static String genCipherData(String data) {
//        String encryptedData = encrypt(data);
//        String SM3Data = getDigest(encryptedData, "SM3");
//        return encryptedData + "@" + SM3Data;
//    }

    public static String genCipherData1(String data) {
        String encryptedData = Base64.encodeBase64String(data.getBytes());
        String md5Data = org.apache.commons.codec.digest.DigestUtils.md5Hex(encryptedData);
        return encryptedData + "@" + md5Data;
    }

    public static String verifyAndDecryptCipherData1(String cipherData) {
        String encryptedData = cipherData.split("@")[0];
        String hexData = cipherData.split("@")[1];
        if (! hexData.equals(org.apache.commons.codec.digest.DigestUtils.md5Hex(encryptedData))) {
            throw new RuntimeException(String.format("Failed to verify the input[%s]", cipherData));
        }
        return new String(Base64.decodeBase64(encryptedData));
    }
}