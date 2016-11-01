package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 11/1/16.
 */
public class APIGetCurrentTimeMillisReply extends APIReply{
    private long currentTimeMillis;

    public long getCurrentTimeMillis() {
        return currentTimeMillis;
    }

    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }
}
