package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.core.db.Q;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;

import javax.persistence.Tuple;

/**
 * @api
 *
 * @category port forwarding
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.portforwarding.APIAttachPortForwardingRuleMsg": {
"ruleUuid": "bc82d5c4f9394c24b7fa19ee611c0857",
"vmNicUuid": "5dfef29a376a49de9e1a887ea9bfe683",
"session": {
"uuid": "7e4e7b4a7b7641c4beef2a4e7c4b5fa2"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.service.portforwarding.APIAttachPortForwardingRuleMsg": {
"ruleUuid": "bc82d5c4f9394c24b7fa19ee611c0857",
"vmNicUuid": "5dfef29a376a49de9e1a887ea9bfe683",
"session": {
"uuid": "7e4e7b4a7b7641c4beef2a4e7c4b5fa2"
},
"timeout": 1800000,
"id": "1e5fc7da29734c6c86249ff6b9190844",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIAttachPortForwardingRuleEvent`
 */
@Action(category = PortForwardingConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/port-forwarding/{ruleUuid}/vm-instances/nics/{vmNicUuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachPortForwardingRuleEvent.class
)
public class APIAttachPortForwardingRuleMsg extends APIMessage {
    /**
     * @desc rule uuid
     */
    @APIParam(resourceType = PortForwardingRuleVO.class, checkAccount = true, operationTarget = true)
    private String ruleUuid;
    /**
     * @desc vm nic uuid the rule attaches to. see :ref:`VmNicInventory`
     */
    @APIParam(resourceType = VmNicVO.class, checkAccount = true, operationTarget = true)
    private String vmNicUuid;

    public String getRuleUuid() {
        return ruleUuid;
    }

    public void setRuleUuid(String ruleUuid) {
        this.ruleUuid = ruleUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
 
    public static APIAttachPortForwardingRuleMsg __example__() {
        APIAttachPortForwardingRuleMsg msg = new APIAttachPortForwardingRuleMsg();
        msg.setVmNicUuid(uuid());
        msg.setRuleUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    Tuple t = Q.New(VmNicVO.class)
                            .select(VmNicVO_.vmInstanceUuid, VmNicVO_.ip)
                            .eq(VmNicVO_.uuid, vmNicUuid).findTuple();

                    String vmUuid = t.get(0, String.class);
                    String ip = t.get(1, String.class);

                    ntfy("Attached port forwarding Rule[uuid:%s]", ruleUuid)
                            .resource(ruleUuid, PortForwardingRuleVO.class.getSimpleName())
                            .context("vmNicUuid", vmNicUuid)
                            .context("vmUuid", vmUuid)
                            .messageAndEvent(that, evt).done();

                    ntfy("Attached a port forwarding rule[%s] to the nic[%s]", ruleUuid, vmNicUuid)
                            .context("ruleUuid", ruleUuid)
                            .resource(vmUuid, VmInstanceVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
