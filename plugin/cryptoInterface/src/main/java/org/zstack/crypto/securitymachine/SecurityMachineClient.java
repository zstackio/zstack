package org.zstack.crypto.securitymachine;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.zstack.core.Platform.operr;

/**
 * Created by LiangHanYu on 2021/10/28 13:58
 */
public interface SecurityMachineClient extends AutoCloseable {
    static final CLogger logger = Utils.getLogger(SecurityMachineClient.class);

    String sm4DecryptNullError = "sm4Decrypt plain is null";
    String sm4EncryptNullError = "sm4Decrypt plain is null";
    String digestNullError = "digest input is null";
    String digestLocalNullError = "digestLocal input is null";
    String certNullError = "cert is null";
    String hmacNullError = "hmac plain is null";
    String attachedVerifyNullError = "attachedVerify input is null";
    /**
     * Return the cipher text signed by the security machine
     *
     * @param input  characters that need to be signed
     * @param certId certificate string
     * @return securityMachineResponse structure contains result String and error code
     */
    SecurityMachineResponse<String> attachedSignature(String input, String certId);

    /**
     * Return to the original text after unsigned by the security machine
     *
     * @param input Signature cipher text
     * @return securityMachineResponse structure contains original text and certificate text
     */
    SecurityMachineResponse<AttachVerifyPair> attachedVerify(String input);

    /**
     * Return SM3 encrypted cipher text
     *
     * @param input the original text
     * @return securityMachineResponse structure contains result String and error code
     */
    SecurityMachineResponse<String> digest(String input);

    /**
     * Return the cipher text after SM3 local encryption without the security machine service,
     *
     * @param input the original text
     * @return securityMachineResponse structure contains result String and error code
     */
    SecurityMachineResponse<String> digestLocal(String input);

    /**
     * Parse the certificate
     *
     * @param cert Certificate byte[]
     * @return X509Certificate object
     */
    default SecurityMachineResponse<X509Certificate> genericCertificate(byte[] cert) {
        SecurityMachineResponse<X509Certificate> response = new SecurityMachineResponse<>();
        if (cert == null) {
            logger.warn(certNullError);
            response.setError(operr(certNullError));
            return response;
        }
        String head = "-----BEGIN CERTIFICATE-----";
        String tail = "-----END CERTIFICATE-----";
        String certString = new String(cert, StandardCharsets.UTF_8).replace("\r", "").replaceAll("\n", "");
        if (certString.indexOf(head) > -1) {
            certString = certString.replaceFirst(head, "").replaceFirst(tail, "");
        }
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("genericCertificate cert is: %s", certString));
        }

        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509", "BC");
            InputStream in = new ByteArrayInputStream(java.util.Base64.getDecoder().decode(certString));
            response.setResult((X509Certificate) certFactory.generateCertificate(in));
        } catch (Exception e) {
            String errorText = "generate certificate failed, detail: %s";
            response.setError(operr(errorText, e.getMessage()));
            logger.warn(String.format(errorText, e.getMessage()));
            return response;
        }
        return response;
    }

    /**
     * SM4 encryption of the string
     *
     * @param plain the original text
     * @return securityMachineResponse structure contains SM4 encrypted string and error code
     */
    SecurityMachineResponse<String> sm4Encrypt(String plain);

    /**
     * SM4 decryption of the string
     *
     * @param plain the Cipher text
     * @return securityMachineResponse structure contains SM4 decrypted string and error code
     */
    SecurityMachineResponse<String> sm4Decrypt(String plain);

    /**
     * Encrypt the string with hmac
     *
     * @param plain the original text
     * @return securityMachineResponse structure contains hmac encrypted string and error code
     */
    SecurityMachineResponse<String> hmac(String plain);

    /**
     * Encrypt large files with hmac
     *
     * @param stream file stream
     * @return securityMachineResponse structure contains encrypted byte[] of large file and error code
     */
    SecurityMachineResponse<byte[]> largeFileHmac(InputStream stream);

    /**
     * Connect remote security machine according to the resource pool uuid
     *
     * @param uuid the secret resource pool uuid
     * @return securityMachineResponse structure contains init result boolean and error code
     */
    SecurityMachineResponse<Boolean> connect(String uuid);

    /**
     * Connect remote security machine according to a security machine
     *
     * @param ip       IP address used to connect to the security machine
     * @param password password required to connect to the security machine
     * @param port     port used to connect to the security machine
     * @return securityMachineResponse structure contains init result boolean and error code
     */
    SecurityMachineResponse<Boolean> connect(String ip, int port, String password);

    /**
     * Get the type of the current security machine client
     *
     * @return the current type of the current cipher machine client. eg: InfoSec
     */
    String getType();

    /**
     * Check if the key exists on the server
     *
     * @return securityMachineResponse structure contains check result boolean and error code
     */
    SecurityMachineResponse<Boolean> isSecretKeyExist(String keyLabel);

    /**
     * Generate a token with a custom name and custom algorithm type
     *
     * @param tokenName  the name of the generated token
     * @param algType    the algType of the generated token
     * @return securityMachineResponse structure contains token String and error code
     */
    SecurityMachineResponse<String> generateToken(String tokenName, String algType);

    /**
     * Generate a Sm4 token with a default name
     *
     * @return securityMachineResponse structure contains token String and error code
     */
    SecurityMachineResponse<String> generateSm4Token(String tokenName);

    /**
     * Generate a dataProtect token with a default name
     *
     * @return securityMachineResponse structure contains token String and error code
     */
    SecurityMachineResponse<String> generateDataProtectToken(String tokenName);

    /**
     * Generate a hmac token with a default name
     *
     * @return securityMachineResponse structure contains token String and error code
     */
    SecurityMachineResponse<String> generateHmacToken(String tokenName);

    /**
     * execute different token generation logic according to the incoming type
     *
     * @return securityMachineResponse structure contains token String and error code
     */
    SecurityMachineResponse<String> generateToken(String keyType);

    SecurityMachineResponse<String> backupToken();
}
