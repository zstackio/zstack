package org.zstack.console;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.console.APIRequestConsoleAccessMsg;
import org.zstack.header.console.ConsoleConstants;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import static org.zstack.core.Platform.operr;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIRequestConsoleAccessMsg) {
            validate((APIRequestConsoleAccessMsg) msg);
        }

        return msg;
    }

    private void validate(APIRequestConsoleAccessMsg msg) {
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.select(VmInstanceVO_.state);
        q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVmInstanceUuid());
        VmInstanceState state = q.findValue();
        if (VmInstanceState.Running != state) {
            throw new ApiMessageInterceptionException(operr("Console is only available when the VM[uuid:%s] is Running, but the current state is %s", msg.getVmInstanceUuid(), state));
        }
        bus.makeTargetServiceIdByResourceUuid(msg, ConsoleConstants.SERVICE_ID, msg.getVmInstanceUuid());
    }
}
