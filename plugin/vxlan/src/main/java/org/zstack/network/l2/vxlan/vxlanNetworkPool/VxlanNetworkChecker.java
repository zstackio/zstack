package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APIMessage;

/**
 * Created by weiwang on 02/05/2017.
 */
public interface VxlanNetworkChecker {
    APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException;
}
