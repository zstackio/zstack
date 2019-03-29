package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;

/**
 * Created by kayo on 2018/7/10.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AccountLoginProcessor implements LoginProcessor {
    public static final LoginType loginType = new LoginType(AccountConstant.LOGIN_TYPE);

    @Autowired
    private PluginRegistry pluginRgty;

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
        return getResourceIdentity(resourceName);
    }

    protected String getResourceIdentity(String name) {
        String resourceIdentity = Q.New(AccountVO.class).select(AccountVO_.uuid).eq(AccountVO_.name, name).findValue();

        if (resourceIdentity == null) {
            for(AccountLoginProcessorExtensionPoint ext : pluginRgty.getExtensionList(AccountLoginProcessorExtensionPoint.class)) {
                resourceIdentity = ext.getResourceIdentity(name);

                if(resourceIdentity != null) {
                    break;
                }
            }
        }

        return resourceIdentity;
    }

    @Override
    public Result getMessageParams(APIMessage message) {
        APILogInByAccountMsg msg = (APILogInByAccountMsg) message;

        Result r = new Result();
        r.setCaptchaUuid(msg.getCaptchaUuid());
        r.setTargetResourceIdentity(getResourceIdentity(msg.getAccountName()));
        r.setVerifyCode(msg.getVerifyCode());

        return r;
    }
}
