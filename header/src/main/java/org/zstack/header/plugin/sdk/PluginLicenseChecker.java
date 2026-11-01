package org.zstack.header.plugin.sdk;

import org.zstack.header.errorcode.ErrorCode;

/**
 * Reserved for licensing feature plugins. Without any checker every plugin is allowed.
 */
@PluginSdk(since = "1.0")
public interface PluginLicenseChecker {
    /**
     * @return null if the plugin is allowed, otherwise why it is not
     */
    ErrorCode check(PluginDescriptor plugin, LicenseCheckPoint point);
}
