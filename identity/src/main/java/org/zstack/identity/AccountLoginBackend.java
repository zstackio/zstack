package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.identity.*;
import org.zstack.header.identity.login.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.err;

/**
 * Created by kayo on 2018/7/10.
 */
public class AccountLoginBackend implements LoginBackend {
    private static final CLogger logger = Utils.getLogger(AccountLoginBackend.class);

    public static final LoginType loginType = new LoginType(AccountConstant.LOGIN_TYPE);

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public LoginType getLoginType() {
        return loginType;
    }

    @Override
    public void login(LoginContext loginContext, ReturnValueCompletion<LoginSessionInfo> completion) {
        AccountVO vo = Q.New(AccountVO.class)
                .eq(AccountVO_.name, loginContext.getUsername())
                .eq(AccountVO_.password, loginContext.getPassword())
                .find();

        String accountType = loginContext.getProperties().get(AccountConstant.ACCOUNT_TYPE);
        LoginSessionInfo info = new LoginSessionInfo();
        boolean accountLogin = accountType == null || AccountConstant.LOGIN_TYPE.equals(accountType);
        if (vo != null && accountLogin) {
            info.setUserUuid(vo.getUuid());
            info.setAccountUuid(vo.getUuid());
            info.setUserType(AccountVO.class.getSimpleName());
        } else {
            logger.debug(String.format("login account type is %s, use AccountLoginExtensionPoint", accountLogin));
            for (AccountLoginExtensionPoint ext : pluginRgty.getExtensionList(AccountLoginExtensionPoint.class)) {
                AccountLoginStruct struct = ext.getLoginEntry(loginContext.getUsername(), loginContext.getPassword(), null);

                if (struct != null) {
                    info.setAccountUuid(struct.getAccountUuid());
                    info.setUserType(struct.getResourceType());
                    info.setUserUuid(struct.getUserUuid());
                    break;
                }
            }
        }

        if (info.getUserUuid() == null) {
            completion.fail(err(IdentityErrors.AUTHENTICATION_ERROR, "wrong account name or password"));
            return;
        }

        completion.success(info);
    }

    @Override
    public Set<String> possibleUserUuidSetForGettingProcedures(LoginContext loginContext) {
        List<String> userUuidList = Q.New(AccountVO.class)
                .select(AccountVO_.uuid)
                .eq(AccountVO_.name, loginContext.getUsername())
                .listValues();
        return new HashSet<>(userUuidList);
    }

    protected String getResourceIdentity(String name) {
        String resourceIdentity = Q.New(AccountVO.class).select(AccountVO_.uuid).eq(AccountVO_.name, name).findValue();

        if (resourceIdentity == null) {
            for(AccountLoginExtensionPoint ext : pluginRgty.getExtensionList(AccountLoginExtensionPoint.class)) {
                AccountLoginStruct struct = ext.getLoginEntryByName(name, null);
                if (struct == null) {
                    continue;
                }

                resourceIdentity = struct.getUserUuid();
                if(resourceIdentity != null) {
                    break;
                }
            }
        }

        return resourceIdentity;
    }

    @Override
    public boolean authenticate(String name, String password) {
        return Q.New(AccountVO.class).eq(AccountVO_.name, name)
                .eq(AccountVO_.password, password).isExists();
    }

    @Override
    public String getUserIdByName(String username) {
        return getResourceIdentity(username);
    }

    private AccountLoginStruct getLoginEntryByName(String username, String accountType) {
        AccountVO vo = Q.New(AccountVO.class).eq(AccountVO_.name, username).find();

        AccountLoginStruct struct = null;
        boolean accountLogin = accountType == null || AccountConstant.LOGIN_TYPE.equals(accountType);
        if (vo != null && accountLogin) {
            struct = new AccountLoginStruct();
            struct.setUserUuid(vo.getUuid());
            struct.setAccountUuid(vo.getUuid());
            struct.setResourceType(AccountVO.class.getSimpleName());
            struct.setLastOpTime(vo.getLastOpDate());
        } else {
            for (AccountLoginExtensionPoint ext : pluginRgty.getExtensionList(AccountLoginExtensionPoint.class)) {
                struct = ext.getLoginEntryByName(username, accountType);
                if (struct != null) {
                    break;
                }
            }
        }

        return struct;
    }

    @Override
    public void collectUserInfoIntoContext(LoginContext loginContext) {
        AccountLoginStruct struct = getLoginEntryByName(loginContext.getUsername(),
                loginContext.getProperties().get(AccountConstant.ACCOUNT_TYPE));

        if (struct == null) {
            return;
        }

        loginContext.setUserUuid(struct.getUserUuid());
        loginContext.setLastUpdatedTime(struct.getLastOpTime());
        loginContext.setUserType(struct.getResourceType());
    }

    @Override
    public List<AdditionalAuthFeature> getRequiredAdditionalAuthFeature() {
        return Arrays.asList(LoginAuthConstant.basicLoginControl, LoginAuthConstant.twoFactor);
    }
}
