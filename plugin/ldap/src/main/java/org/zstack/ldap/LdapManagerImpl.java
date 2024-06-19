package org.zstack.ldap;

import java.sql.SQLIntegrityConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.HardcodedFilter;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.AbstractService;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.identity.*;
import org.zstack.header.identity.login.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.imports.AccountImportsConstant;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefInventory;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO_;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.entity.SyncDeletedAccountStrategy;
import org.zstack.identity.imports.message.DestroyThirdPartyAccountSourceMsg;
import org.zstack.identity.imports.message.SyncThirdPartyAccountMsg;
import org.zstack.ldap.api.*;
import org.zstack.ldap.driver.LdapSearchSpec;
import org.zstack.ldap.entity.LdapServerInventory;
import org.zstack.ldap.entity.LdapServerType;
import org.zstack.ldap.entity.LdapServerVO;
import org.zstack.ldap.entity.LdapServerVO_;
import org.zstack.ldap.header.LdapAccountSourceSpec;
import org.zstack.ldap.message.CreateLdapAccountSourceMsg;
import org.zstack.ldap.message.UpdateLdapAccountSourceMsg;
import org.zstack.ldap.message.UpdateLdapAccountSourceReply;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.PersistenceException;
import java.util.*;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.ldap.LdapConstant.CURRENT_LDAP_UUID_NONE;
import static org.zstack.ldap.LdapErrors.MORE_THAN_ONE_LDAP_SERVER;

/**
 * Created by miao on 16-9-6.
 */
public class LdapManagerImpl extends AbstractService implements LdapManager, LoginBackend {
    private static final CLogger logger = Utils.getLogger(LdapManagerImpl.class);

    private static final LoginType loginType = new LoginType(LdapConstant.LOGIN_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddLdapServerMsg) {
            handle((APIAddLdapServerMsg) msg);
        } else if (msg instanceof APIDeleteLdapServerMsg) {
            handle((APIDeleteLdapServerMsg) msg);
        } else if (msg instanceof APIGetLdapEntryMsg) {
            handle((APIGetLdapEntryMsg) msg);
        } else if(msg instanceof APIGetCandidateLdapEntryForBindingMsg){
            handle((APIGetCandidateLdapEntryForBindingMsg) msg);
        } else if (msg instanceof APISyncAccountsFromLdapServerMsg) {
            handle((APISyncAccountsFromLdapServerMsg) msg);
        } else if (msg instanceof APICreateLdapBindingMsg) {
            handle((APICreateLdapBindingMsg) msg);
        } else if (msg instanceof APIDeleteLdapBindingMsg) {
            handle((APIDeleteLdapBindingMsg) msg);
        } else if (msg instanceof APIUpdateLdapServerMsg) {
            handle((APIUpdateLdapServerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional
    private AccountThirdPartyAccountSourceRefInventory bindLdapAccount(String accountUuid, String ldapUid) {
        AccountThirdPartyAccountSourceRefVO ref = new AccountThirdPartyAccountSourceRefVO();
        ref.setAccountUuid(accountUuid);
        ref.setAccountSourceUuid(ldapUtil.getLdapServer().getUuid());
        ref.setCredentials(ldapUid);
        ref = dbf.persistAndRefresh(ref);
        return AccountThirdPartyAccountSourceRefInventory.valueOf(ref);
    }


    public String getId() {
        return bus.makeLocalServiceId(LdapConstant.SERVICE_ID);
    }

    public boolean start() {
        installGlobalConfigValidator();
        return true;
    }

    public boolean stop() {
        return true;
    }

    private void installGlobalConfigValidator() {
        LdapGlobalConfig.CURRENT_LDAP_SERVER_UUID.installValidateExtension((category, name, oldValue, newValue) -> {
            if (CURRENT_LDAP_UUID_NONE.equals(newValue)) {
                return;
            }

            boolean exists = Q.New(LdapServerVO.class)
                    .eq(LdapServerVO_.uuid, newValue)
                    .isExists();
            if (!exists) {
                throw new GlobalConfigException(String.format(
                        "failed to update GlobalConfig[%s]: invalid LdapServer[uuid=%s]",
                        LdapGlobalConfig.CURRENT_LDAP_SERVER_UUID.getName(), newValue));
            }
        });
    }

    @Override
    public boolean isValid(String uid, String password) {
        return ldapUtil.isValid(uid, password);
    }

    private void handle(APIAddLdapServerMsg msg) {
        APIAddLdapServerEvent event = new APIAddLdapServerEvent(msg.getId());

        LdapAccountSourceSpec spec = new LdapAccountSourceSpec();
        spec.setUuid(Platform.getUuid());
        spec.setServerName(msg.getName());
        spec.setDescription(msg.getDescription());
        spec.setType(LdapConstant.LOGIN_TYPE);
        spec.setUrl(msg.getUrl());
        spec.setBaseDn(msg.getBase());
        spec.setLogInUserName(msg.getUsername());
        spec.setLogInPassword(msg.getPassword());
        spec.setEncryption(LdapEncryptionType.valueOf(msg.getEncryption()));
        spec.setServerType(LdapServerType.valueOf(msg.getServerType()));
        spec.setCreateAccountStrategy(SyncCreatedAccountStrategy.valueOf(msg.getSyncCreatedAccountStrategy()));
        spec.setDeleteAccountStrategy(SyncDeletedAccountStrategy.valueOf(msg.getSyncDeletedAccountStrategy()));
        spec.setUsernameProperty(msg.getUsernameProperty());
        if (msg.getFilter() != null) {
            spec.setFilter(msg.getFilter());
        }

        CreateLdapAccountSourceMsg innerMsg = new CreateLdapAccountSourceMsg();
        innerMsg.setSpec(spec);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, LdapServerVO.class.getName());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    final LdapServerVO server = dbf.findByUuid(spec.getUuid(), LdapServerVO.class);
                    event.setInventory(LdapServerInventory.valueOf(server));
                } else {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIDeleteLdapServerMsg msg) {
        APIDeleteLdapServerEvent event = new APIDeleteLdapServerEvent(msg.getId());
        DestroyThirdPartyAccountSourceMsg innerMsg = new DestroyThirdPartyAccountSourceMsg();
        innerMsg.setUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, msg.getUuid());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIGetLdapEntryMsg msg) {
        APIGetLdapEntryReply reply = new APIGetLdapEntryReply();

        LdapSearchSpec spec = new LdapSearchSpec();
        if (msg.getLdapServerUuid() == null) {
            final ErrorableValue<String> currentLdapServerUuid = findCurrentLdapServerUuid();
            if (currentLdapServerUuid.isSuccess()) {
                spec.setLdapServerUuid(currentLdapServerUuid.result);
            } else {
                reply.setError(currentLdapServerUuid.error);
                bus.reply(msg, reply);
                return;
            }
        } else {
            spec.setLdapServerUuid(msg.getLdapServerUuid());
        }

        spec.setFilter(msg.getLdapFilter());
        spec.setCount(msg.getLimit());
        reply.setInventories(ldapUtil.searchLdapEntry(spec));

        bus.reply(msg, reply);
    }

    private void handle(APIGetCandidateLdapEntryForBindingMsg msg) {
        APIGetLdapEntryReply reply = new APIGetLdapEntryReply();

        LdapSearchSpec spec = new LdapSearchSpec();
        if (msg.getLdapServerUuid() == null) {
            final ErrorableValue<String> currentLdapServerUuid = findCurrentLdapServerUuid();
            if (currentLdapServerUuid.isSuccess()) {
                spec.setLdapServerUuid(currentLdapServerUuid.result);
            } else {
                reply.setError(currentLdapServerUuid.error);
                bus.reply(msg, reply);
                return;
            }
        } else {
            spec.setLdapServerUuid(msg.getLdapServerUuid());
        }

        List<String> boundLdapEntryList = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, spec.getLdapServerUuid())
                .select(AccountThirdPartyAccountSourceRefVO_.credentials)
                .listValues();
        Set<String> boundLdapEntrySet = new HashSet<>(boundLdapEntryList);

        spec.setFilter(new AndFilter().and(new HardcodedFilter(msg.getLdapFilter())).toString());
        spec.setCount(msg.getLimit());
        spec.setResultFilter(dn -> !boundLdapEntrySet.contains(dn));
        reply.setInventories(ldapUtil.searchLdapEntry(spec));

        bus.reply(msg, reply);
    }

    private void handle(APICreateLdapBindingMsg msg) {
        APICreateLdapBindingEvent evt = new APICreateLdapBindingEvent(msg.getId());

        // account check
        SimpleQuery<AccountVO> sq = dbf.createQuery(AccountVO.class);
        sq.add(AccountVO_.uuid, SimpleQuery.Op.EQ, msg.getAccountUuid());
        AccountVO avo = sq.find();
        if (avo == null) {
            evt.setError(err(LdapErrors.CANNOT_FIND_ACCOUNT,
                    String.format("cannot find the specified account[uuid:%s]", msg.getAccountUuid())));
            bus.publish(evt);
            return;
        }

        // bind op
        LdapTemplateContextSource ldapTemplateContextSource = ldapUtil.readLdapServerConfiguration();
        String fullDn = msg.getLdapUid();
        if (!ldapUtil.validateDnExist(ldapTemplateContextSource, fullDn)) {
            throw new OperationFailureException(err(LdapErrors.UNABLE_TO_GET_SPECIFIED_LDAP_UID,
                    "cannot find dn[%s] on LDAP/AD server[Address:%s, BaseDN:%s].", fullDn,
                    String.join(", ", ldapTemplateContextSource.getLdapContextSource().getUrls()),
                    ldapTemplateContextSource.getLdapContextSource().getBaseLdapPathAsString()));
        }
        try {
            evt.setInventory(bindLdapAccount(msg.getAccountUuid(), fullDn));
            logger.info(String.format("create ldap binding[ldapUid=%s, account=%s] success", fullDn, msg.getAccountUuid()));
        } catch (PersistenceException e) {
            if (ExceptionDSL.isCausedBy(e, SQLIntegrityConstraintViolationException.class)) {
                evt.setError(err(LdapErrors.BIND_SAME_LDAP_UID_TO_MULTI_ACCOUNT,
                        "The ldap uid has been bound to an account. "));
            } else {
                throw e;
            }
        }
        bus.publish(evt);
    }

    private void handle(APIDeleteLdapBindingMsg msg) {
        APIDeleteLdapBindingEvent evt = new APIDeleteLdapBindingEvent(msg.getId());

        dbf.removeByPrimaryKey(msg.getUuid(), AccountThirdPartyAccountSourceRefVO.class);

        bus.publish(evt);
    }

    private void handle(APIUpdateLdapServerMsg msg) {
        APIUpdateLdapServerEvent event = new APIUpdateLdapServerEvent(msg.getId());

        LdapAccountSourceSpec spec = new LdapAccountSourceSpec();
        spec.setUuid(msg.getLdapServerUuid());
        spec.setServerName(msg.getName());
        spec.setDescription(msg.getDescription());
        spec.setType(LdapConstant.LOGIN_TYPE);
        spec.setUrl(msg.getUrl());
        spec.setBaseDn(msg.getBase());
        spec.setLogInUserName(msg.getUsername());
        spec.setLogInPassword(msg.getPassword());
        spec.setEncryption(msg.getEncryption() == null ? null : LdapEncryptionType.valueOf(msg.getEncryption()));
        spec.setServerType(msg.getServerType() == null ? null : LdapServerType.valueOf(msg.getServerType()));
        spec.setCreateAccountStrategy(msg.getSyncCreatedAccountStrategy() == null ? null :
                SyncCreatedAccountStrategy.valueOf(msg.getSyncCreatedAccountStrategy()));
        spec.setDeleteAccountStrategy(msg.getSyncDeletedAccountStrategy() == null ? null :
                SyncDeletedAccountStrategy.valueOf(msg.getSyncDeletedAccountStrategy()));
        spec.setUsernameProperty(msg.getUsernameProperty());
        spec.setFilter(msg.getFilter());

        UpdateLdapAccountSourceMsg innerMsg = new UpdateLdapAccountSourceMsg();
        innerMsg.setSpec(spec);
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, spec.getUuid());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    event.setInventory(((UpdateLdapAccountSourceReply) reply).getInventory());
                } else {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APISyncAccountsFromLdapServerMsg msg) {
        SyncThirdPartyAccountMsg innerMsg = new SyncThirdPartyAccountMsg();
        innerMsg.setSourceUuid(msg.getUuid());
        bus.makeTargetServiceIdByResourceUuid(innerMsg, AccountImportsConstant.SERVICE_ID, msg.getUuid());
        bus.send(innerMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                APISyncAccountsFromLdapServerEvent event = new APISyncAccountsFromLdapServerEvent(msg.getId());
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private ErrorableValue<String> findCurrentLdapServerUuid() {
        final List<String> ldapServerUuidList = Q.New(LdapServerVO.class)
                .select(LdapServerVO_.uuid)
                .listValues();
        if (ldapServerUuidList.size() > 1) {
            return ErrorableValue.ofErrorCode(err(MORE_THAN_ONE_LDAP_SERVER,
                    "failed to find ldap server: System has more than one ldap server, but no LDAP server specified"));
        }
        if (ldapServerUuidList.isEmpty()) {
            return ErrorableValue.ofErrorCode(operr("failed to find ldap server: no LDAP server exists"));
        }

        return ErrorableValue.of(ldapServerUuidList.get(0));
    }

    @Override
    public LoginType getLoginType() {
        return loginType;
    }

    @Override
    public void login(LoginContext loginContext, ReturnValueCompletion<LoginSessionInfo> completion) {
        String ldapLoginName = loginContext.getUsername();
        if (!isValid(ldapLoginName, loginContext.getPassword())) {
            completion.fail(err(IdentityErrors.AUTHENTICATION_ERROR,
                    "Login validation failed in LDAP"));
            return;
        }

        LdapTemplateContextSource ldapTemplateContextSource = ldapUtil.readLdapServerConfiguration();
        String dn = ldapUtil.getFullUserDn(ldapTemplateContextSource.getLdapTemplate(), ldapUtil.getLdapUseAsLoginName(), ldapLoginName);
        AccountThirdPartyAccountSourceRefVO vo = ldapUtil.findLdapAccountRefVO(dn);

        if (vo == null) {
            completion.fail(err(IdentityErrors.AUTHENTICATION_ERROR,
                    "The ldapUid does not have a binding account."));
            return;
        }

        SimpleQuery<AccountVO> sq = dbf.createQuery(AccountVO.class);
        sq.add(AccountVO_.uuid, SimpleQuery.Op.EQ, vo.getAccountUuid());
        AccountVO avo = sq.find();
        if (avo == null) {
            completion.fail(operr(
                    "Account[uuid:%s] Not Found!!!", vo.getAccountUuid()));
            return;
        }

        LoginSessionInfo info = new LoginSessionInfo();
        info.setUserUuid(vo.getAccountUuid());
        info.setAccountUuid(vo.getAccountUuid());
        info.setUserType(AccountVO.class.getSimpleName());
        completion.success(info);
    }

    @Override
    public boolean authenticate(String username, String password) {
        return ldapUtil.isValid(username, password);
    }

    @Override
    public String getUserIdByName(String username) {
        return ldapUtil.getFullUserDn(username);
    }

    @Override
    public void collectUserInfoIntoContext(LoginContext loginContext) {
        loginContext.setUserUuid(getUserIdByName(loginContext.getUsername()));
    }

    @Override
    public List<AdditionalAuthFeature> getRequiredAdditionalAuthFeature() {
        return Collections.singletonList(LoginAuthConstant.basicLoginControl);
    }
}
