package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class HostApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        if (msg instanceof HostMessage) {
            HostMessage hmsg = (HostMessage)msg;
            bus.makeTargetServiceIdByResourceUuid(msg, HostConstant.SERVICE_ID, hmsg.getHostUuid());
        }
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        setServiceId(msg);

        if (msg instanceof APIAddHostMsg) {
            validate((APIAddHostMsg) msg);
        }
        return msg;
    }

    private void validate(APIAddHostMsg msg) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.add(HostVO_.managementIp, Op.EQ, msg.getManagementIp());
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("there has been a host having managementIp[%s]", msg.getManagementIp())
            ));
        }
    }
}
