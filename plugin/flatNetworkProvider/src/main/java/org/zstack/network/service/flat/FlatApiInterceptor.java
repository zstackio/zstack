package org.zstack.network.service.flat;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.AccountManager;
import org.zstack.identity.rbac.CheckIfAccountCanAccessResource;

import java.util.Collections;

import static org.zstack.core.Platform.operr;

/**
 * Created by Qi Le on 2019/9/9
 */
public class FlatApiInterceptor implements ApiMessageInterceptor {

    @Autowired
    private AccountManager accountManager;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIGetL3NetworkIpStatisticMsg) {
            validate((APIGetL3NetworkIpStatisticMsg) msg);
        }

        return msg;
    }

    private void validate(APIGetL3NetworkIpStatisticMsg msg) {
        if (accountManager.isAdmin(msg.getSession())) {
            return;
        }
        String accountUuid = msg.getSession().getAccountUuid();
        if (StringUtils.isBlank(accountUuid)) {
            throw new ApiMessageInterceptionException(Platform.argerr("Session/account uuid is not valid."));
        }
        if (!CheckIfAccountCanAccessResource.check(Collections.singletonList(msg.getL3NetworkUuid()), accountUuid).isEmpty()) {
            throw new ApiMessageInterceptionException(
                    operr("the account[uuid:%s] has no access to the resource[uuid:%s, type:L3NetworkVO]",
                    accountUuid, msg.getL3NetworkUuid()));
        }
    }
}
