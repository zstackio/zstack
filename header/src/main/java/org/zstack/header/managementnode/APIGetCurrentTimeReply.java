package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;

import java.util.Map;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 11/1/16.
 */
public class APIGetCurrentTimeReply extends APIReply {
    private Map<String, Long> currentTime;

    public Map<String, Long> getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(Map<String, Long> currentTime) {
        this.currentTime = currentTime;
    }
}
