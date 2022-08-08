package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.identity.*;
import org.zstack.header.message.APIMessage;

import java.sql.Timestamp;

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
            if (resourceIdentity == null) {
                //account not found in DB
                resourceIdentity = String.format(AccountConstant.NO_EXIST_ACCOUNT, name);
            }
        }

        return resourceIdentity;
    }

    protected Timestamp getLastOperatedTime(String resourceIdentity) {
        Timestamp lastUpdatedTime = Q.New(AccountVO.class).select(AccountVO_.lastOpDate).eq(AccountVO_.uuid, resourceIdentity).findValue();

        if (lastUpdatedTime == null) {
            for(AccountLoginProcessorExtensionPoint ext : pluginRgty.getExtensionList(AccountLoginProcessorExtensionPoint.class)) {
                lastUpdatedTime = ext.getLastOperatedTime(resourceIdentity);

                if(lastUpdatedTime != null) {
                    break;
                }
            }
        }

        return lastUpdatedTime;
    }

    @Override
    public Result getMessageParams(APIMessage message) {
        APILogInByAccountMsg msg = (APILogInByAccountMsg) message;

        Result r = new Result();
        r.setCaptchaUuid(msg.getCaptchaUuid());
        r.setTargetResourceIdentity(getResourceIdentity(msg.getAccountName()));
        r.setVerifyCode(msg.getVerifyCode());
        r.setLastUpdatedTime(getLastOperatedTime(r.getTargetResourceIdentity()));

        return r;
    }

    @Override
    public boolean authenticate(String name, String password) {
        AccountVO acvo = Q.New(AccountVO.class).eq(AccountVO_.name, name)
                .eq(AccountVO_.password, password).find();

        if (acvo != null) {
            return true;
        }
        return false;
    }
}
