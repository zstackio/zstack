package org.zstack.header.core.external.plugin;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Sent to the other management nodes after a feature plugin is installed or uninstalled:
 * store the package (if any) in the local plugin repository, then reconcile the local plugin files.
 */
public class SyncFeaturePluginPackageMsg extends NeedReplyMessage {
    // the plugin whose load failure fails the sync, null if none: a peer reports the other plugins in their status
    private String pluginId;
    private String packageName;
    private String sha256;
    @NoLogging
    private String content;

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
