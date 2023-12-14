package org.zstack.header.securitymachine.secretresourcepool;

import org.zstack.header.message.Message;

/**
 * Created by LiangHanYu on 2021/11/4 17:48
 */
public interface SecretResourcePool {
    String idPrefix = "SecretResourcePool-";
    String dataProtectTokenSuffix = "_ConfidentialitySM4";
    String hmacTokenSuffix = "_IntegrityHmacSM3";

    static String buildId(String uuid) {
        return idPrefix + uuid;
    }

    static String buildHmacToken(String uuid) {
        return uuid.substring(0, 8) + hmacTokenSuffix;
    }

    static String buildDataProtectToken(String uuid) {
        return uuid.substring(0, 8) + dataProtectTokenSuffix;
    }

    void handleMessage(Message msg);
    String getId();
}
