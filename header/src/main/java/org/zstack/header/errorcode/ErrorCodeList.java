package org.zstack.header.errorcode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/4/19.
 */
public class ErrorCodeList extends ErrorCode {
    private List<ErrorCode> causes = new ArrayList<>();

    public List<ErrorCode> getCauses() {
        return causes;
    }

    public void setCauses(List<ErrorCode> causes) {
        this.causes = causes;
    }
}
