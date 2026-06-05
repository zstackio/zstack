package org.zstack.zcenter.accounts.compute;

import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.identity.AccountSource;
import org.zstack.header.message.APIMessage;
import org.zstack.zcenter.accounts.api.APICreateSessionForZCenterAccountMsg;

import static org.zstack.core.Platform.argerr;

public class ZCenterAccountApiInterceptor implements ApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateSessionForZCenterAccountMsg) {
            validate((APICreateSessionForZCenterAccountMsg) msg);
        }
        return msg;
    }

    private void validate(APICreateSessionForZCenterAccountMsg msg) {
        if (msg.getAccountUuid() == null && msg.getAccountName() == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "either accountUuid or accountName must be provided"));
        }

        if (msg.getAccountUuid() != null && msg.getAccountName() != null) {
            throw new ApiMessageInterceptionException(argerr(
                    "accountUuid and accountName are mutually exclusive, please provide only one"));
        }

        if (msg.getAccountName() != null && msg.getSource() == null) {
            msg.setSource(AccountSource.Local.name());
        }
    }
}
