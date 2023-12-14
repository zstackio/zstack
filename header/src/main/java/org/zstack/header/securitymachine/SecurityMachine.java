package org.zstack.header.securitymachine;

import org.zstack.header.message.Message;

/**
 * Created by LiangHanYu on 2021/11/16 16:07
 */
public interface SecurityMachine {
    String idPrefix = "SecurityMachine-";

    static String buildId(String uuid) {
        return idPrefix + uuid;
    }

    void handleMessage(Message msg);
    String getId();
}
