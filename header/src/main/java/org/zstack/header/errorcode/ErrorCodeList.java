package org.zstack.header.errorcode;

import java.util.List;

/**
 * Created by xing5 on 2016/4/19.
 */
public class ErrorCodeList extends ErrorCode {
    private List<ErrorCode> causes;

    @Override
    public String toString() {
        return causes == null ? String.format("ErrorCode [code = %s, description = %s, details = %s]", this.getCode(), this.getDescription(), this.getDetails()) :
                String.format("ErrorCode [code = %s, description = %s, details = %s, causes = %s]", this.getCode(), this.getDescription(), this.getDetails(), causes.toString());
    }

    public List<ErrorCode> getCauses() {
        return causes;
    }

    public void setCauses(List<ErrorCode> causes) {
        this.causes = causes;
    }
}
