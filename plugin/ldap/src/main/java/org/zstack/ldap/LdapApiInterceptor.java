package org.zstack.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.portal.apimediator.PortalSystemTags;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

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
        }

        setServiceId(msg);

        return msg;
    }

    private void validate(APIAddLdapServerMsg msg) {
        if (msg.hasSystemTag(PortalSystemTags.VALIDATION_ONLY.getTagFormat())) {
            LdapServerInventory inv = new LdapServerInventory();
            inv.setName(msg.getName());
            inv.setDescription(msg.getDescription());
            inv.setUrl(msg.getUrl());
            inv.setBase(msg.getBase());
            inv.setUsername(msg.getUsername());
            inv.setPassword(msg.getPassword());
            inv.setEncryption(msg.getEncryption());

            boolean success = testAddLdapServerConnection(inv);
            if (!success) {
                throw new ApiMessageInterceptionException(
                        errf.instantiateErrorCode(LdapErrors.TEST_LDAP_CONNECTION_FAILED,
                                "Test ldap server connection failed. "));
            }
        }
    }

    private boolean testAddLdapServerConnection(LdapServerInventory inv) {
        LdapTemplateContextSource ldapTemplateContextSource = new LdapUtil().loadLdap(inv);

        try {
            AndFilter filter = new AndFilter();
            filter.and(new EqualsFilter("uid", ""));
            ldapTemplateContextSource.getLdapTemplate().authenticate("", filter.toString(), "");
            logger.info("LDAP connection was successful");
        } catch (Exception e) {
            logger.info("Cannot connect to LDAP server");
            logger.debug(e.toString());
            return false;
        }

        return true;
    }

}
