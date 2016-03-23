package org.zstack.core.gc;

/**
 * Created by xing5 on 2016/3/23.
 */
public class GCEventTrigger {
    private String eventPath;
    private String code;
    private String codeName;

    public String getEventPath() {
        return eventPath;
    }

    public void setEventPath(String eventPath) {
        this.eventPath = eventPath;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCodeName() {
        return codeName;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
    }
}
