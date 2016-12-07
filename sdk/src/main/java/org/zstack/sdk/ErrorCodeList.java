package org.zstack.sdk;

import java.util.List;

/**
 * Created by xing5 on 2016/4/19.
 */
public class ErrorCodeList extends ErrorCode {
    private List<ErrorCode> causes;

    public List<ErrorCode> getCauses() {
        return causes;
    }

    public void setCauses(List<ErrorCode> causes) {
        this.causes = causes;
    }
}
