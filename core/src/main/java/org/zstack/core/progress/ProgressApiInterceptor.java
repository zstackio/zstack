package org.zstack.core.progress;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.core.progress.APIGetTaskProgressMsg;
import org.zstack.header.core.progress.ProgressConstants;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/12/12.
 */
public class ProgressApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(ProgressApiInterceptor.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIGetTaskProgressMsg) {
            validate((APIGetTaskProgressMsg) msg);
        }
        setServiceId(msg);
        return msg;
    }

    private void validate(APIGetTaskProgressMsg msg) {
        if (msg.getResourceUuid() == null) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    "resourceUuid cannot be null"
            ));
        }
    }

    private void setServiceId(APIMessage msg) {
        if (msg instanceof APIGetTaskProgressMsg) {
            APIGetTaskProgressMsg pmsg = (APIGetTaskProgressMsg)msg;
            msg.setServiceId(ProgressConstants.SERVICE_ID);
        }
    }
}
