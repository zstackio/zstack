package org.zstack.header.rest;

import org.springframework.http.HttpEntity;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.gson.JSONObjectUtil;

public abstract class JsonAsyncRESTCallback<T> extends AsyncRESTCallback {
    public JsonAsyncRESTCallback() {
    }

    public JsonAsyncRESTCallback(AsyncBackup...backup) {
        super(backup);
    }

    public abstract void fail(ErrorCode err);
    public abstract void success(T ret);
    public abstract Class<T> getReturnClass();

    @Override
    public final void success(HttpEntity<String> entity) {
        throw new CloudRuntimeException("this method should not be called");
    }
}
