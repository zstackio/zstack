package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;
import org.zstack.utils.JsonWrapper;

import java.util.HashMap;
import java.util.Map;

public class RestoreVmReply extends MessageReply {
    private Map<String, JsonWrapper> extensionData = new HashMap<>();

    public <T> T getExtensionData(String key, Class<?> clazz) {
        JsonWrapper<T> wrapper = extensionData.get(key);
        return wrapper == null ? null : wrapper.get();
    }

    public void putExtensionData(String key, Object data) {
        extensionData.put(key, JsonWrapper.wrap(data));
    }
}
