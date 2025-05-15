package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.MessageReply;

/**
 * @author: jianjun.zhang
 * @date: 2025-05-13
 **/
public interface VirtualRouterSNATExtensionPoint {
    String getSNATWhiteList(String vrUuid);
}
