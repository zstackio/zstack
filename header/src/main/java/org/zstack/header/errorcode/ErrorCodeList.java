package org.zstack.header.errorcode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by xing5 on 2016/4/19.
 */
public class ErrorCodeList extends ErrorCode {
    private List<ErrorCode> causes = Collections.synchronizedList(new ArrayList<>());

    public List<ErrorCode> getCauses() {
        return causes;
    }

    public void setCauses(List<ErrorCode> causes) {
        this.causes = causes;
    }


    /**
     *
     * @param t
     * @return if the causes of the two objects both have no value,
     * even the other object is not ErrorCodeList but ErrorCode,
     * return true.
     * if not compare their values.
     */

    @Override
    public boolean equals(Object t) {
        if(super.equals(t)){
            if(t instanceof ErrorCodeList){
                ErrorCodeList other = (ErrorCodeList)t;
                if((this.causes == null && other.causes.isEmpty()) || this.causes.isEmpty() && other.causes == null){
                    return true;
                } else {
                    return Objects.equals(this.causes, other.causes);
                }
            }
            return causes == null || causes.isEmpty();
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = result * 31 + (causes == null ? 0 : causes.hashCode());

        return result;
    }
}
