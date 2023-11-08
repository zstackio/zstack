package org.zstack.crypto.securitymachine.secretresourcepool;

/**
 * Created by LiangHanYu on 2021/11/5 18:00
 */
public interface CreateInfoSecSecretResourcePoolMessage extends CreateSecretResourcePoolMessage {
    int getConnectionMode();
}
