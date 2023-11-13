package org.zstack.crypto.securitymachine;

import org.zstack.header.message.Message;

/**
 * Created by LiangHanYu on 2021/11/3 11:01
 */
public interface SecurityMachineManager {
    SecurityMachineClient getSecurityMachineClient(String securityMachineType);

    SecurityMachineClientFactory getSecurityMachineClientFactory(String securityMachineType);

    void handleMessage(Message msg);
}
