package org.zstack.mediator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.network.service.eip.APIAttachEipMsg;
import org.zstack.network.service.eip.APICreateEipMsg;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.eip.EipVO_;
import org.zstack.network.service.portforwarding.APIAttachPortForwardingRuleMsg;
import org.zstack.network.service.portforwarding.APICreatePortForwardingRuleMsg;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO_;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ApiValidator implements GlobalApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    @Override
    public List<Class> getMessageClassToIntercept() {
        List<Class> ret = new ArrayList<>();
        ret.add(APICreateEipMsg.class);
        ret.add(APIAttachEipMsg.class);
        ret.add(APICreatePortForwardingRuleMsg.class);
        ret.add(APIAttachPortForwardingRuleMsg.class);
        return ret;
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateEipMsg) {
            validate((APICreateEipMsg) msg);
        } else if (msg instanceof APIAttachEipMsg) {
            validate((APIAttachEipMsg) msg);
        } else if (msg instanceof APICreatePortForwardingRuleMsg) {
            validate((APICreatePortForwardingRuleMsg) msg);
        } else if (msg instanceof APIAttachPortForwardingRuleMsg) {
            validate((APIAttachPortForwardingRuleMsg) msg);
        }

        return msg;
    }

    private void validate(APIAttachPortForwardingRuleMsg msg) {
        isVmNicUsedByEip(msg.getVmNicUuid());
    }

    private void validate(APICreatePortForwardingRuleMsg msg) {
        if (msg.getVmNicUuid() != null) {
            isVmNicUsedByEip(msg.getVmNicUuid());
        }
    }

    private void isVmNicUsedByPortForwarding(String vmNicUuid) {
        SimpleQuery<PortForwardingRuleVO> q = dbf.createQuery(PortForwardingRuleVO.class);
        q.select(PortForwardingRuleVO_.uuid);
        q.add(PortForwardingRuleVO_.vmNicUuid, Op.EQ, vmNicUuid);
        List<String> uuids = q.listValue();
        if (!uuids.isEmpty()) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("there are already some port forwarding rules[uuids: %s] attached to vm nic[uuid:%s], cannot attach eip",
                            uuids, vmNicUuid)
            ));
        }
    }

    private void isVmNicUsedByEip(String vmNicUuid) {
        SimpleQuery<EipVO> q = dbf.createQuery(EipVO.class);
        q.select(EipVO_.uuid);
        q.add(EipVO_.vmNicUuid, Op.EQ, vmNicUuid);
        String eipUuid = q.findValue();
        if (eipUuid != null) {
            throw new ApiMessageInterceptionException(errf.stringToOperationError(
                    String.format("there are already an eip[uuid:%s] attached to vm nic[uuid:%s], cannot attach port forwarding rule",
                            eipUuid, vmNicUuid)
            ));
        }
    }


    private void validate(APIAttachEipMsg msg) {
        isVmNicUsedByPortForwarding(msg.getVmNicUuid());
    }

    private void validate(APICreateEipMsg msg) {
        if (msg.getVmNicUuid() != null) {
            isVmNicUsedByPortForwarding(msg.getVmNicUuid());
        }
    }
}
