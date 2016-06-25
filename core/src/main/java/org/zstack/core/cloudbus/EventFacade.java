package org.zstack.core.cloudbus;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:29 PM
 * To change this template use File | Settings | File Templates.
 */
public interface EventFacade {
    void on(String path, AutoOffEventCallback cb);

    void on(String path, EventCallback cb);

    void on(String path, Runnable runnable);

    void off(Object cb);

    void onLocal(String path, AutoOffEventCallback cb);

    void onLocal(String path, EventCallback cb);

    void onLocal(String path, Runnable runnable);

    void offLocal(Object cb);

    void fire(String path, Object data);

    boolean isFromThisManagementNode(Map tokens);

    String META_DATA_MANAGEMENT_NODE_ID = "metadata::managementNodeId";
}
