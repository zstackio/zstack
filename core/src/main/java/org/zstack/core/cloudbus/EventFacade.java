package org.zstack.core.cloudbus;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:29 PM
 * To change this template use File | Settings | File Templates.
 */
public interface EventFacade {
    public void on(String path, AutoOffEventCallback cb);

    public void on(String path, EventCallback cb);

    public void on(String path, Runnable runnable);

    public void off(Object cb);

    public void fire(String path, Object data);
}
