package org.zstack.identity;

import org.zstack.core.db.Q;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;

/**
 * Created by kayo on 2018/7/10.
 */
public class AccountLoginProcessor implements LoginProcessor {
    public static final LoginType loginType = new LoginType(AccountConstant.LOGIN_TYPE);

    @Override
    public LoginType getLoginType() {
        return loginType;
    }

    @Override
    public Class getMessageClass() {
        return APILogInByAccountMsg.class;
    }

    @Override
    public String resourceChecker(String resourceName) {
        return Q.New(AccountVO.class).select(AccountVO_.uuid).eq(AccountVO_.name, resourceName).findValue();
    }

    @Override
    public Result getMessageParams(APIMessage message) {
        APILogInByAccountMsg msg = (APILogInByAccountMsg) message;

        String resourceIdentity = Q.New(AccountVO.class).select(AccountVO_.uuid).eq(AccountVO_.name, msg.getAccountName()).findValue();

        Result r = new Result();
        r.setCaptchaUuid(msg.getCaptchaUuid());
        r.setTargetResourceIdentity(resourceIdentity);
        r.setVerifyCode(msg.getVerifyCode());

        return r;
    }
}
