package org.zstack.core.cloudbus;

import java.util.Map;

/**
 */
public interface AutoOffEventCallback<T> {
    boolean run(Map<String, String> tokens, T data);
}
