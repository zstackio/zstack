package org.zstack.storage.primary.iscsi;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by frank on 6/10/2015.
 */
public class IscsiBtrfsPrimaryStorageAsyncCallReply extends MessageReply {
    @NoJsonSchema
    private Object response;

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public <T> T toResponse(Class<T> clz) {
        return JSONObjectUtil.rehashObject(response, clz);
    }
}
