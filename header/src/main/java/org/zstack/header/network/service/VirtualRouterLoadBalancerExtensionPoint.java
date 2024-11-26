package org.zstack.header.network.service;

import org.zstack.header.core.Completion;

/**
 * Created by boce.wang on 11/25/2024.
 */
public interface VirtualRouterLoadBalancerExtensionPoint {
    void afterRefreshLoadBalancerListener(String vrUuid, Completion completion);
}
