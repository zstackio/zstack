package org.zstack.plugin.example;

import org.zstack.header.plugin.sdk.PluginUsageChecker;

import java.util.Collections;
import java.util.List;

/**
 * Tells whether the plugin is in use; replace with the plugin's own logic or remove it and use lifecycle.inUse.
 */
public class ExampleUsageChecker implements PluginUsageChecker {
    @Override
    public List<String> inUseReasons() {
        return Collections.emptyList();
    }
}
