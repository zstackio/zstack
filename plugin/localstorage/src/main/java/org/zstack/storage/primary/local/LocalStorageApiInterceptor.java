package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;

/**
 * Created by frank on 7/1/2015.
 */
public class LocalStorageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddLocalPrimaryStorageMsg) {
            validate((APIAddLocalPrimaryStorageMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddLocalPrimaryStorageMsg msg) {
        if (!msg.getUrl().startsWith("/")) {
            throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                    String.format("the url[%s] is not an absolute path starting with '/'", msg.getUrl())
            ));
        }
    }
}
