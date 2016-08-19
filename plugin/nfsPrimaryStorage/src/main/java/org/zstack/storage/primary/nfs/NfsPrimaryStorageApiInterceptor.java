package org.zstack.storage.primary.nfs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;

/**
 */
public class NfsPrimaryStorageApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddNfsPrimaryStorageMsg) {
            validate((APIAddNfsPrimaryStorageMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddNfsPrimaryStorageMsg msg) {
        ErrorCode err = new NfsApiParamChecker().checkUrl(msg.getZoneUuid(), msg.getUrl());
        if (err != null) {
            throw new ApiMessageInterceptionException(err);
        }
    }
}
