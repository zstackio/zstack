package org.zstack.kvm;

import org.zstack.utils.gson.JSONObjectUtil;

import java.util.LinkedHashMap;

/**
 * Created by xing5 on 2016/4/19.
 */
public class KvmResponseWrapper {
    private LinkedHashMap response;

    public KvmResponseWrapper(LinkedHashMap response) {
        this.response = response;
    }

    public <T> T getResponse(Class<T> type) {
        return JSONObjectUtil.rehashObject(response, type);
    }
}
