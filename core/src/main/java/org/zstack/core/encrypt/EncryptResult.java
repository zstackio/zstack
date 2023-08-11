package org.zstack.core.encrypt;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by LiangHanYu on 2021/11/14 19:16
 */
public class EncryptResult<T> {
    T result;
    ErrorCode error = null;

    public EncryptResult() {
    }

    public EncryptResult(T result) {
        this.result = result;
    }

    public EncryptResult(ErrorCode error) {
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