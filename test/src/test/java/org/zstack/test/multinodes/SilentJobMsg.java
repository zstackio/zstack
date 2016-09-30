package org.zstack.test.multinodes;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class SilentJobMsg extends NeedReplyMessage {
    private boolean restartable;
    private int jobNum;

    public boolean isRestartable() {
        return restartable;
    }

    public void setRestartable(boolean restartable) {
        this.restartable = restartable;
    }

    public int getJobNum() {
        return jobNum;
    }

    public void setJobNum(int jobNum) {
        this.jobNum = jobNum;
    }
}
