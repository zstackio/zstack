package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * Created by Majin on 2017/10/19.
 */
public class GetVmSnapshotMaxNumReply extends MessageReply {
    private Integer maxNum;

    public void setMaxNum(Integer maxNum) {
        this.maxNum = maxNum;
    }

    public Integer getMaxNum() {
        return maxNum;
    }
}
