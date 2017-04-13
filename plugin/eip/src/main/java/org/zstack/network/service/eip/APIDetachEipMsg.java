package org.zstack.network.service.eip;

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
 *
 * detach eip from vm nic
 *
 * @category eip
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *{
"org.zstack.network.service.eip.APIDetachEipMsg": {
"uuid": "69198105fd7a40778fba1759b923545c",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
}
}
}
 * @msg
 * {
"org.zstack.network.service.eip.APIDetachEipMsg": {
"uuid": "69198105fd7a40778fba1759b923545c",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
},
"timeout": 1800000,
"id": "2bbc35d895b445da8b1a869f9b351458",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIDetachEipEvent`
 */
@Action(category = EipConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/eips/{uuid}/vm-instances/nics",
        method = HttpMethod.DELETE,
        responseClass = APIDetachEipEvent.class
)
public class APIDetachEipMsg extends APIMessage implements EipMessage {
    /**
     * @desc eip uuid
     */
    @APIParam(resourceType = EipVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getEipUuid() {
        return uuid;
    }
 
    public static APIDetachEipMsg __example__() {
        APIDetachEipMsg msg = new APIDetachEipMsg();
        msg.setUuid(uuid());

        return msg;
    }

    
    public ApiNotification __notification__() {
        APIMessage that = this;
        
        return new ApiNotification() {
            String ip;
            String vmUuid;
            String vmNicUuid;
            String eip;

            @Override
            public void before() {
                Tuple tuple = SQL.New("select nic.ip, nic.vmInstanceUuid, nic.uuid, eip.vipIp from VmNicVO nic, EipVO eip" +
                        " where eip.vmNicUuid = nic.uuid and eip.uuid = :uuid", Tuple.class)
                        .param("uuid", getEipUuid()).find();
                ip = tuple.get(0, String.class);
                vmUuid = tuple.get(1, String.class);
                vmNicUuid = tuple.get(2, String.class);
                eip = tuple.get(3, String.class);
            }

            @Override
            public void after(APIEvent evt) {
                ntfy("detached from a nic[ip:%s] of the VM[uuid:%s]", ip, vmUuid)
                        .resource(getUuid(), EipVO.class.getSimpleName())
                        .context("vmUuid", vmUuid)
                        .context("vmNicUuid", vmNicUuid)
                        .messageAndEvent(that, evt)
                        .done();

                ntfy("detached an EIP[%s] from the nic[ip:%s]", eip, ip)
                        .resource(vmUuid, VmInstanceVO.class.getSimpleName())
                        .context("eipUuid", getEipUuid())
                        .context("vmNicUuid", vmNicUuid)
                        .messageAndEvent(that, evt)
                        .done();
            }
        };
    }
}
