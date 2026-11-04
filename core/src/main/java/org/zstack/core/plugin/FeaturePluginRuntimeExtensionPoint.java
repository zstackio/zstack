package org.zstack.core.plugin;

/**
 * Lets a platform registry outside the core module add and drop the parts of a feature plugin that is loaded or
 * unloaded at runtime. Platform internal, not part of the plugin SDK.
 */
public interface FeaturePluginRuntimeExtensionPoint {
    /**
     * Throws when the plugin cannot be registered; the loader then undoes what was registered before.
     */
    void featurePluginLoaded(LoadedFeaturePlugin plugin);

    /**
     * Must not throw: an unload goes on whatever a registry reports. Called in the reverse order of loading.
     */
    void featurePluginUnloading(LoadedFeaturePlugin plugin);
}
