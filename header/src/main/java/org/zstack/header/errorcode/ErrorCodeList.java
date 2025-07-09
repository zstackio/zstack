package org.zstack.header.errorcode;

/**
 * Created by xing5 on 2016/4/19.
 */
@Deprecated
public class ErrorCodeList extends ErrorCode {
    @Override
    public ErrorCodeList withOpaque(String key, Object value) {
        super.withOpaque(key, value);
        return this;
    }
}
