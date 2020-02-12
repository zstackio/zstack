package org.zstack.header.message;

public abstract class APIGetMessage extends APISyncCallMessage {
    private Integer limit = 1000;
    private Integer start = 0;

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }
}
