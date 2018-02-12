package org.zstack.network.service.portforwarding;

import org.springframework.http.HttpMethod;
import org.zstack.core.db.SQL;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

import javax.persistence.Tuple;

/**
 * @api
 * detach a rule from vm nic
 *
 * @category port forwarding
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.portforwarding.APIDetachPortForwardingRuleMsg": {
"uuid": "26679c3eb6694a5e8e3528e5f7afe6d1",
"session": {
"uuid": "cc8751da40054ff2ac9d99f074edb875"
}
}
}
 * @msg
 * {
"org.zstack.network.service.portforwarding.APIDetachPortForwardingRuleMsg": {
"uuid": "26679c3eb6694a5e8e3528e5f7afe6d1",
"session": {
"uuid": "cc8751da40054ff2ac9d99f074edb875"
},
"timeout": 1800000,
"id": "2562e4f0e1dd4bd6a68d402b2b4c824a",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIDetachPortForwardingRuleEvent`
 */
@Action(category = PortForwardingConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/port-forwarding/{uuid}/vm-instances/nics",
        method = HttpMethod.DELETE,
        responseClass = APIDetachPortForwardingRuleEvent.class
)
public class APIDetachPortForwardingRuleMsg extends APIMessage {
    /**
     * @desc rule uuid
     */
    @APIParam(resourceType = PortForwardingRuleVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDetachPortForwardingRuleMsg __example__() {
        APIDetachPortForwardingRuleMsg msg = new APIDetachPortForwardingRuleMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            String ip;
            String vmUuid;
            String vmNicUuid;

            @Override
            public void before() {
                Tuple tuple = SQL.New("select nic.ip, nic.vmInstanceUuid, nic.uuid from VmNicVO nic, PortForwardingRuleVO pfr" +
                        " where pfr.vmNicUuid = nic.uuid and pfr.uuid = :uuid", Tuple.class)
                        .param("uuid", getUuid()).find();
                ip = tuple.get(0, String.class);
                vmUuid = tuple.get(1, String.class);
                vmNicUuid = tuple.get(2, String.class);
            }

            @Override
            public void after(APIEvent evt) {
                ntfy("Detached from a nic[ip:%s] of the VM[uuid:%s]", ip, vmUuid)
                        .resource(getUuid(), PortForwardingRuleVO.class.getSimpleName())
                        .context("vmUuid", vmUuid)
                        .context("vmNicUuid", vmNicUuid)
                        .messageAndEvent(that, evt)
                        .done();

                ntfy("Detached a port forwarding rule[%s] from the nic[ip:%s]", getUuid(), ip)
                        .resource(vmUuid, VmInstanceVO.class.getSimpleName())
                        .context("portforwardingRuleUuid", getUuid())
                        .context("vmNicUuid", vmNicUuid)
                        .messageAndEvent(that, evt)
                        .done();
            }
        };
    }

}
