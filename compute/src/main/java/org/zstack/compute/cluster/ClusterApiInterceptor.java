package org.zstack.compute.cluster;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.cluster.*;
import org.zstack.header.message.APIMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClusterApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof ClusterMessage) {
            ClusterMessage cmsg = (ClusterMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, ClusterConstant.SERVICE_ID, cmsg.getClusterUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);

        if (msg instanceof APIDeleteClusterMsg) {
            validate((APIDeleteClusterMsg) msg);
        }

        return msg;
    }

    private void validate(APIDeleteClusterMsg msg) {
        if (!dbf.isExist(msg.getUuid(), ClusterVO.class)) {
            APIDeleteClusterEvent evt = new APIDeleteClusterEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }
    }
}
