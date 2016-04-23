package org.zstack.core.componentloader;

import org.zstack.utils.function.Function;

import java.util.List;

public interface PluginRegistry {
    List<PluginExtension> getExtensionByInterfaceName(String interfaceName);

    <T> List<T> getExtensionList(Class<T> clazz);

    <T, K> void saveExtensionAsMap(Class<T> clazz, Function<K, T> func);

    <T> T getExtensionFromMap(Object key, Class<T> clazz);
    
    public static final String PLUGIN_REGISTRY_BEAN_NAME = "zstack.PluginRegistry";
    public static final String PLUGIN_REGISTRYIMPL_PLUGINS_FIELD_NAME = "extensions";
}
