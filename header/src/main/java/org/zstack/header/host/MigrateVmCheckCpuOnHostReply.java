package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

/**
 * Created by LiangHanYu on 2021/7/29 15:30
 */
public class MigrateVmCheckCpuOnHostReply extends MessageReply {
    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
