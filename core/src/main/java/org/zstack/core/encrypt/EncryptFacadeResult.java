package org.zstack.core.encrypt;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by LiangHanYu on 2021/11/14 19:16
 */
public class EncryptFacadeResult<T> {
    T result;
    ErrorCode error = null;

    public EncryptFacadeResult() {
    }

    public EncryptFacadeResult(T result) {
        this.result = result;
    }

    public EncryptFacadeResult(ErrorCode error) {
        this.error = error;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
    }
}