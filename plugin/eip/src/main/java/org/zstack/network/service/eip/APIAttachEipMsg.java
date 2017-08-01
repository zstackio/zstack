package org.zstack.network.service.eip;

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
 * attach eip to a vm nic
 *
 * @category eip
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.service.eip.APIAttachEipMsg": {
"eipUuid": "69198105fd7a40778fba1759b923545c",
"vmNicUuid": "0bdb4fb64baa4c4d85d39c9f43773342",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.service.eip.APIAttachEipMsg": {
"eipUuid": "69198105fd7a40778fba1759b923545c",
"vmNicUuid": "0bdb4fb64baa4c4d85d39c9f43773342",
"session": {
"uuid": "b8a93f786c474493b34e560b49a8da1f"
},
"timeout": 1800000,
"id": "3df75c42d240472d8fc1e7edecd83149",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIAttachEipEvent`
 */
@Action(category = EipConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/eips/{eipUuid}/vm-instances/nics/{vmNicUuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachEipEvent.class
)
public class APIAttachEipMsg extends APIMessage implements EipMessage {
    /**
     * @desc eip uuid
     */
    @APIParam(resourceType = EipVO.class, checkAccount = true, operationTarget = true)
    private String eipUuid;
    /**
     * @desc vm nic uuid. See :ref:`VmNicInventory`
     */
    @APIParam(resourceType = VmNicVO.class, checkAccount = true, operationTarget = true)
    private String vmNicUuid;

    public String getEipUuid() {
        return eipUuid;
    }

    public void setEipUuid(String eipUuid) {
        this.eipUuid = eipUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
 
    public static APIAttachEipMsg __example__() {
        APIAttachEipMsg msg = new APIAttachEipMsg();
        msg.setEipUuid(uuid());
        msg.setVmNicUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                Tuple t = Q.New(VmNicVO.class)
                        .select(VmNicVO_.vmInstanceUuid, VmNicVO_.ip)
                        .eq(VmNicVO_.uuid, vmNicUuid).findTuple();

                String vmUuid = t.get(0, String.class);
                String ip = t.get(1, String.class);

                ntfy("Attached to the nic[%s]", ip)
                        .resource(eipUuid, EipVO.class.getSimpleName())
                        .context("vmNicUuid", vmNicUuid)
                        .context("vmUuid", vmUuid)
                        .messageAndEvent(that, evt).done();

                String eip = Q.New(EipVO.class)
                        .select(EipVO_.vipIp)
                        .eq(EipVO_.uuid, eipUuid).findValue();

                ntfy("Attached an Eip[%s] to the nic[%s]", eip, ip)
                        .context("eipUuid", eipUuid)
                        .resource(vmUuid, VmInstanceVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
