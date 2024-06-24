package org.zstack.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.message.APIMessage;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO_;
import org.zstack.ldap.api.APIAddLdapServerMsg;
import org.zstack.ldap.api.APICreateLdapBindingMsg;
import org.zstack.ldap.api.APIDeleteLdapBindingMsg;
import org.zstack.ldap.api.APIGetCandidateLdapEntryForBindingMsg;
import org.zstack.ldap.api.APIGetLdapEntryMsg;
import org.zstack.ldap.entity.LdapServerVO;
import org.zstack.ldap.entity.LdapServerVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.*;

/**
 */
public class LdapApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(LdapApiInterceptor.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private LdapManager ldapManager;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddLdapServerMsg) {
            validate((APIAddLdapServerMsg) msg);
        } else if(msg instanceof APICreateLdapBindingMsg){
            validate((APICreateLdapBindingMsg) msg);
        } else if(msg instanceof APIDeleteLdapBindingMsg){
            validate((APIDeleteLdapBindingMsg) msg);
        } else if(msg instanceof APIGetLdapEntryMsg){
            validate((APIGetLdapEntryMsg) msg);
        } else if(msg instanceof APIGetCandidateLdapEntryForBindingMsg){
            validate((APIGetCandidateLdapEntryForBindingMsg) msg);
        }

        return msg;
    }

    private void validate(APIAddLdapServerMsg msg) {
        LdapServerVO ldap = new LdapServerVO();
        ldap.setUrl(msg.getUrl());
        ldap.setBase(msg.getBase());
        ldap.setUsername(msg.getUsername());
        ldap.setPassword(msg.getPassword());
        ldap.setEncryption(msg.getEncryption());

        ErrorCode errorCode = LdapManager.ldapUtil.testLdapServerConnection(ldap);
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(
                    err(LdapErrors.TEST_LDAP_CONNECTION_FAILED, errorCode.getDetails()));
        }
    }

    private void validate(APICreateLdapBindingMsg msg){
        final LdapServerVO ldap;
        if (msg.getLdapServerUuid() == null) {
            ldap = findCurrentLdapServerOrThrow();
            msg.setLdapServerUuid(ldap.getUuid());
        }

        boolean refExists = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountUuid, msg.getAccountUuid())
                .isExists();
        if (refExists) {
            throw new ApiMessageInterceptionException(
                    argerr("the ldap uid has already been bound to account[uuid=%s]", msg.getAccountUuid()));
        }
    }

    private void validate(APIDeleteLdapBindingMsg msg) {
        String accountSourceUuid = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .select(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountUuid, msg.getAccountUuid())
                .findValue();
        if (accountSourceUuid == null) {
            logger.debug(String.format("maybe account[uuid=%s] has been already unbound from third party account source",
                    msg.getAccountUuid()));
            return;
        }

        boolean exists = Q.New(LdapServerVO.class)
                .eq(LdapServerVO_.uuid, accountSourceUuid)
                .isExists();
        if (!exists) {
            throw new ApiMessageInterceptionException(argerr(
                    "account[uuid=%s] is binding to non-LDAP third party account source", msg.getAccountUuid()));
        }
        msg.setLdapServerUuid(accountSourceUuid);
    }

    private void validate(APIGetLdapEntryMsg msg) {
        final LdapServerVO ldap;
        if (msg.getLdapServerUuid() == null) {
            ldap = findCurrentLdapServerOrThrow();
            msg.setLdapServerUuid(ldap.getUuid());
        }
    }

    private void validate(APIGetCandidateLdapEntryForBindingMsg msg){
        final LdapServerVO ldap;
        if (msg.getLdapServerUuid() == null) {
            ldap = findCurrentLdapServerOrThrow();
            msg.setLdapServerUuid(ldap.getUuid());
        }
    }

    private LdapServerVO findCurrentLdapServerOrThrow() {
        final ErrorableValue<LdapServerVO> ldapServer = ldapManager.findCurrentLdapServer();
        if (!ldapServer.isSuccess()) {
            throw new ApiMessageInterceptionException(ldapServer.error);
        }
        return ldapServer.result;
    }
}
