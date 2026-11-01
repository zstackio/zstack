package org.zstack.header.core.external.plugin;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Sent by a management node that misses the package of a desired feature plugin to another management node.
 */
public class FetchFeaturePluginPackageMsg extends NeedReplyMessage {
    private String sha256;

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }
}
