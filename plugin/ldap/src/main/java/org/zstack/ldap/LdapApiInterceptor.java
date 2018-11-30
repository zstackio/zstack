package org.zstack.ldap;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import java.util.List;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.core.Platform.err;

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
        } else if(msg instanceof APIUpdateLdapServerMsg){
            validate((APIUpdateLdapServerMsg) msg);
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

        ErrorCode errorCode = testAddLdapServerConnection(inv);
        if (errorCode != null) {
            throw new ApiMessageInterceptionException(
                    err(LdapErrors.TEST_LDAP_CONNECTION_FAILED,
                            errorCode.getDetails()));
        }

        validateLdapType(msg.getSystemTags());
    }

    private void validate(APIUpdateLdapServerMsg msg){
        validateLdapType(msg.getSystemTags());
    }

    private void validate(APICreateLdapBindingMsg msg){
        validateLdapServerExist();
    }

    private void validate(APIGetLdapEntryMsg msg){
        validateLdapServerExist();
    }

    private void validate(APIGetCandidateLdapEntryForBindingMsg msg){
        validateLdapServerExist();
    }

    private void validateLdapType(List<String> systemTags){
        if(systemTags == null || systemTags.isEmpty()){
            return;
        }

        String type = SystemTagUtils.findTagValue(systemTags, LdapSystemTags.LDAP_SERVER_TYPE, LdapSystemTags.LDAP_SERVER_TYPE_TOKEN);
        if(StringUtils.isEmpty(type)){
            return;
        }

        if(!(LdapConstant.OpenLdap.TYPE.equals(type) || LdapConstant.WindowsAD.TYPE.equals(type))){
            throw new ApiMessageInterceptionException(
                    argerr("Wrong LdapServerType[%s], valid values: [%,%s]", type, LdapConstant.OpenLdap.TYPE, LdapConstant.WindowsAD.TYPE)
            );
        }
    }

    private void validateLdapServerExist(){
        if(!Q.New(LdapServerVO.class).isExists()){
            throw new ApiMessageInterceptionException(argerr("There is no ldap server in the system, Please add a ldap server first."));
        }
    }

    private ErrorCode testAddLdapServerConnection(LdapServerInventory inv) {
        LdapTemplateContextSource ldapTemplateContextSource = new LdapUtil().loadLdap(inv);

        try {
            AndFilter filter = new AndFilter();
            // Any search conditions
            filter.and(new EqualsFilter(LdapConstant.LDAP_UID_KEY, ""));
            ldapTemplateContextSource.getLdapTemplate().authenticate("", filter.toString(), "");
            logger.info("LDAP connection was successful");
        } catch (Exception e) {
            logger.debug("Cannot connect to LDAP server", e);
            return operr("Cannot connect to LDAP server, %s", e.toString());
        }

        return null;
    }

}
