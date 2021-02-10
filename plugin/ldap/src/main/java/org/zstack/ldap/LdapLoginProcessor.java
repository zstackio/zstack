package org.zstack.ldap;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.LoginProcessor;
import org.zstack.header.identity.LoginType;
import org.zstack.header.message.APIMessage;

/**
 *  * Created by kayo on 2018/7/9.
 *   */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LdapLoginProcessor implements LoginProcessor {
    public static final LoginType loginType = new LoginType(LdapConstant.LOGIN_TYPE);
    public static LdapUtil ldapUtil = Platform.New(() -> new LdapUtil(AccountConstant.LOGIN_TYPE));

    @Override
    public LoginType getLoginType() {
        return loginType;
    }

    @Override
    public Class getMessageClass() {
        return APILogInByLdapMsg.class;
    }

    public String resourceChecker(String resourceName) {
        String dn = LdapManager.ldapUtil.getFullUserDn(resourceName);

        LdapAccountRefVO vo = LdapManager.ldapUtil.findLdapAccountRefVO(dn);

        if (vo == null) {
            return null;
        }

        return dn;
    }

    @Override
    public Result getMessageParams(APIMessage message) {
        Result r = new Result();

        APILogInByLdapMsg msg = (APILogInByLdapMsg) message;
        String dn = LdapManager.ldapUtil.getFullUserDn(msg.getUid());

        LdapAccountRefVO vo = LdapManager.ldapUtil.findLdapAccountRefVO(dn);

        if (vo == null) {
            r.setTargetResourceIdentity(null);
        } else {
            r.setTargetResourceIdentity(dn);
        }

        r.setVerifyCode(msg.getVerifyCode());
        r.setCaptchaUuid(msg.getCaptchaUuid());

        return r;
    }

    @Override
    public boolean authenticate(String name, String password) {
        return ldapUtil.isValid(name, password);
    }
}
