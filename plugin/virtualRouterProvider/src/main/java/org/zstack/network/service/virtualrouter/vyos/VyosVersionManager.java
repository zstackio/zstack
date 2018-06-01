package org.zstack.network.service.virtualrouter.vyos;

import org.zstack.header.core.Completion;

/**
 * Created by shixin on 2018/05/22.
 */
public interface VyosVersionManager {
    void vyosRouterVersionCheck(String vrUuid, Completion completion);
}
