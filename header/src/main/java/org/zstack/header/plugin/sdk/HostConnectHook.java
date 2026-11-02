package org.zstack.header.plugin.sdk;

import org.zstack.header.core.Completion;

/**
 * Called while a host connects, after its facts are collected. A failure fails the connection.
 */
@PluginSdk(since = "1.0")
public interface HostConnectHook {
    void onConnecting(HostConnectContext ctx, Completion completion);
}
