package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.console.APIRequestConsoleAccessMsg;
import org.zstack.header.console.ConsoleConstants;
import org.zstack.header.message.APIMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIRequestConsoleAccessMsg) {
            bus.makeTargetServiceIdByResourceUuid(msg, ConsoleConstants.SERVICE_ID, ((APIRequestConsoleAccessMsg) msg).getVmInstanceUuid());
        }

        return msg;
    }
}
