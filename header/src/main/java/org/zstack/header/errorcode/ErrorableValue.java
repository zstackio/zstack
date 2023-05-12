package org.zstack.header.errorcode;

import java.util.Objects;

/**
 * Created by Wenhao.Zhang on 22/11/21
 */
public class ErrorableValue<T> {
    public final T result;
    public final ErrorCode error;

    public static <T> ErrorableValue<T> of(T result) {
        return new ErrorableValue<>(result, null);
    }

    public static <T> ErrorableValue<T> ofErrorCode(ErrorCode error) {
        return new ErrorableValue<>(null,
                Objects.requireNonNull(error, "errorCode in ErrorableValue can not be null"));
    }

    protected ErrorableValue(T result, ErrorCode error) {
        this.result = result;
        this.error = error;
    }

    public boolean isSuccess() {
        return error == null;
    }
}
