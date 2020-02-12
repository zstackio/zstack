package org.zstack.header.message;

public abstract class APIGetMessage extends APISyncCallMessage {
    private Integer limit = 1000;
    private Integer offset = 0;

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }
}
