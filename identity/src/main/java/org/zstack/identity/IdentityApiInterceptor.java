package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountMessage;
import org.zstack.header.message.APIMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class IdentityApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof AccountMessage) {
            AccountMessage amsg = (AccountMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, AccountConstant.SERVICE_ID, amsg.getAccountUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);
        return msg;
    }
}
