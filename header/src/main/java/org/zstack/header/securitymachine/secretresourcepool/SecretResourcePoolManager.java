package org.zstack.header.securitymachine.secretresourcepool;

import org.zstack.header.message.Message;

/**
 * Created by LiangHanYu on 2021/11/4 16:53
 */
public interface SecretResourcePoolManager {
    void handleMessage(Message msg);

    SecretResourcePoolFactory getSecretResourcePoolFactory(String secretResourcePoolModel);
}
