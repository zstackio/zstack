package org.zstack.header.core.encrypt;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: DaoDao
 * @Date: 2021/11/4
 */
public class StartDataProtectionMsg extends NeedReplyMessage {
    private String encryptType = "InfosecEncryptDriver";

    public String getEncryptType() {
        return encryptType;
    }

    public void setEncryptType(String encryptType) {
        this.encryptType = encryptType;
    }
}
