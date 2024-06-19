package org.zstack.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.ldap.api.APIAddLdapServerMsg;
import org.zstack.ldap.api.APICreateLdapBindingMsg;
import org.zstack.ldap.api.APIGetCandidateLdapEntryForBindingMsg;
import org.zstack.ldap.api.APIGetLdapEntryMsg;
import org.zstack.ldap.driver.LdapTemplateContextSource;
import org.zstack.ldap.entity.LdapServerInventory;
import org.zstack.ldap.entity.LdapServerVO;
import org.zstack.ldap.entity.LdapServerVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

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

    private void setServiceId(APIMessage msg) {
        if (msg instanceof LdapMessage) {
            LdapMessage emsg = (LdapMessage) msg;
            bus.makeTargetServiceIdByResourceUuid(msg, LdapConstant.SERVICE_ID, emsg.getEipUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIAddLdapServerMsg) {
            validate((APIAddLdapServerMsg) msg);
        } else if(msg instanceof APICreateLdapBindingMsg){
            validate((APICreateLdapBindingMsg) msg);
        } else if(msg instanceof APIGetLdapEntryMsg){
            validate((APIGetLdapEntryMsg) msg);
        } else if(msg instanceof APIGetCandidateLdapEntryForBindingMsg){
            validate((APIGetCandidateLdapEntryForBindingMsg) msg);
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIAddLdapServerMsg msg) {
        LdapServerInventory inv = new LdapServerInventory();
        inv.setName(msg.getName());
        inv.setDescription(msg.getDescription());
        inv.setUrl(msg.getUrl());
        inv.setBase(msg.getBase());
        inv.setUsername(msg.getUsername());
        inv.setPassword(msg.getPassword());
        inv.setEncryption(msg.getEncryption());
        validateLdapServer(inv);
    }

    private void validate(APICreateLdapBindingMsg msg){
        validateLdapServerExist();
    }

    private void validate(APIGetLdapEntryMsg msg){
        validateLdapServerExist();

        if (msg.getLdapServerUuid() != null) {
            LdapServerVO ldapServerVO = Q.New(LdapServerVO.class)
                    .eq(LdapServerVO_.uuid, msg.getLdapServerUuid())
                    .find();
            LdapServerInventory inv = LdapServerInventory.valueOf(ldapServerVO);
            validateLdapServer(inv);
        }
    }

    private void validateLdapServer(LdapServerInventory inv) {
        ErrorCode errorCode = testAddLdapServerConnection(inv);
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(
                    err(LdapErrors.TEST_LDAP_CONNECTION_FAILED,
                            errorCode.getDetails()));
        }
    }

    private void validate(APIGetCandidateLdapEntryForBindingMsg msg){
        validateLdapServerExist();
    }

    private void validateLdapServerExist(){
        if(!Q.New(LdapServerVO.class).isExists()){
            throw new ApiMessageInterceptionException(argerr("There is no LDAP/AD server in the system, Please add a LDAP/AD server first."));
        }
    }

    public ErrorCode testAddLdapServerConnection(LdapServerInventory inv) {
        Map<String, Object> properties = new HashMap<>();
        String timeout = Integer.toString(LdapGlobalProperty.LDAP_ADD_SERVER_CONNECT_TIMEOUT);
        properties.put("com.sun.jndi.ldap.connect.timeout", timeout);
        LdapTemplateContextSource ldapTemplateContextSource = LdapManager.ldapUtil.loadLdap(inv, properties);

        try {
            AndFilter filter = new AndFilter();
            // Any search conditions
            filter.and(new EqualsFilter(LdapConstant.LDAP_UID_KEY, ""));
            ldapTemplateContextSource.getLdapTemplate().authenticate("", filter.toString(), "");
            logger.info("LDAP connection was successful");
        } catch (AuthenticationException e) {
            logger.debug("Cannot connect to LDAP/AD server, Invalid Credentials, please checkout User DN and password", e);
            return operr("Cannot connect to LDAP/AD server, Invalid Credentials, please checkout User DN and password");
        } catch (CommunicationException e) {
            logger.debug("Cannot connect to LDAP/AD server, communication false, please checkout IP, port and Base DN", e);
            return operr("Cannot connect to LDAP/AD server, communication false, please checkout IP, port and Base DN");
        } catch (Exception e) {
            logger.debug("Cannot connect to LDAP/AD server", e);
            return operr("Cannot connect to LDAP/AD server, %s", e.toString());
        }

        return null;
    }

}
