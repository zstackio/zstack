package org.zstack.header.securitymachine;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Created by LiangHanYu on 2021/11/12 13:58
 */
public class SecurityMachineResponse<T> {
    T result;
    ErrorCode error = null;

    public SecurityMachineResponse() {
    }

    public SecurityMachineResponse(T result) {
        this.result = result;
    }

    public SecurityMachineResponse(ErrorCode error) {
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