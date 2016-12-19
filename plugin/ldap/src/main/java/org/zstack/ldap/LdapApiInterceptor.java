package org.zstack.ldap;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;

/**
 */
public class LdapApiInterceptor implements ApiMessageInterceptor {
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

        } else if (msg instanceof APICreateLdapBindingMsg) {

        } else if (msg instanceof APIDeleteLdapBindingMsg) {

        }

        setServiceId(msg);

        return msg;
    }

}
