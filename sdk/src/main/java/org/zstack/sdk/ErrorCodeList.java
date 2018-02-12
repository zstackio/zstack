package org.zstack.sdk;

public class ErrorCodeList extends ErrorCode {

    public java.util.List<ErrorCode> causes;
    public void setCauses(java.util.List<ErrorCode> causes) {
        this.causes = causes;
    }
    public java.util.List<ErrorCode> getCauses() {
        return this.causes;
    }

}
