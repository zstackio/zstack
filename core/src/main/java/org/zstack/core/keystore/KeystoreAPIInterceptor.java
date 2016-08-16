package org.zstack.core.keystore;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.scheduler.APIDeleteSchedulerMsg;
import org.zstack.core.scheduler.APIUpdateSchedulerMsg;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.message.APIMessage;

/**
 * Created by miao on 16-8-15.
 */
public class KeystoreAPIInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof KeystoreMessage) {
            KeystoreMessage ksmsg = (KeystoreMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, KeystoreConstant.SERVICE_ID, ksmsg.getKeystoreUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);
        return msg;
    }

}
