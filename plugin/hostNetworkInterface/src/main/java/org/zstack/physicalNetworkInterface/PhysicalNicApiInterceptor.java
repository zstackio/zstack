package org.zstack.physicalNetworkInterface;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.physicalNetworkInterface.header.APIConfigurePhysicalNicMsg;
import org.zstack.physicalNetworkInterface.header.APIRecoverPhysicalNicMsg;

public class PhysicalNicApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIConfigurePhysicalNicMsg) {
            validate((APIConfigurePhysicalNicMsg)msg);
        } else if (msg instanceof APIRecoverPhysicalNicMsg) {
            validate((APIRecoverPhysicalNicMsg)msg);
        }
        return msg;
    }
    private void validate(APIConfigurePhysicalNicMsg msg) {
        //todo: sriov split and smart nic split only can do one

    }
    private void validate(APIRecoverPhysicalNicMsg msg) {
        //todo: check if recover type is correct
    }
}
