package org.zstack.test.multinodes;

import org.zstack.header.message.LocalEvent;

/**
 */
public class SilentJobEvent extends LocalEvent {
    private String jobUuid;
    private boolean canGo;

    @Override
    public String getSubCategory() {
        return "SilentJobEvent";
    }

    public String getJobUuid() {
        return jobUuid;
    }

    public void setJobUuid(String jobUuid) {
        this.jobUuid = jobUuid;
    }

    public boolean isCanGo() {
        return canGo;
    }

    public void setCanGo(boolean canGo) {
        this.canGo = canGo;
    }
}
