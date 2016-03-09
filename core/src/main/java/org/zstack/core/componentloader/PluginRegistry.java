package org.zstack.core.componentloader;

import java.util.List;

public interface PluginRegistry {
    List<PluginExtension> getExtensionByInterfaceName(String interfaceName);

    void processExtensionByInterfaceName(String interfaceName, ExtensionProcessor processor, Object[] args);

    void processExtensions(List<PluginExtension> exts, ExtensionProcessor processor, Object[] args);

    <T> List<T> getExtensionList(Class<T> clazz);
    
    public static final String PLUGIN_REGISTRY_BEAN_NAME = "zstack.PluginRegistry";
    public static final String PLUGIN_REGISTRYIMPL_PLUGINS_FIELD_NAME = "extensions";
}
