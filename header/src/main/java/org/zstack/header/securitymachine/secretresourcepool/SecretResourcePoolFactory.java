package org.zstack.header.securitymachine.secretresourcepool;

/**
 * Created by LiangHanYu on 2021/11/4 14:48
 */
public interface SecretResourcePoolFactory {

    String getSecretResourcePoolModel();

    SecretResourcePoolVO createSecretResourcePool(SecretResourcePoolVO vo, CreateSecretResourcePoolMessage msg);

    SecretResourcePoolInventory getSecretResourcePoolInventory(String uuid);

    SecretResourcePool getSecretResourcePool(SecretResourcePoolVO vo);
}
