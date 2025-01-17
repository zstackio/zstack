package org.zstack.header.core.external.plugin;

import org.zstack.header.errorcode.ErrorCode;

public interface PluginDriversExtensionPoint {
    ErrorCode validateDeletePluginDrivers(String pluginUuid);
}
