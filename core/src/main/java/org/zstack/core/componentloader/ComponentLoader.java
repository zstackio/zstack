package org.zstack.core.componentloader;

public interface ComponentLoader {
    <T> T getComponent(Class<T> clazz);

    <T> T getComponentNoExceptionWhenNotExisting(Class<T> clazz);

    <T> T getComponent(String className);
    
    <T> T getComponentByBeanName(String beanName);
    
    PluginRegistry getPluginRegistry();
}
