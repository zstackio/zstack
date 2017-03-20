package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api
 * attach security group to a l3Network
 *
 * @category security group
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.network.securitygroup.APIAttachSecurityGroupToL3NetworkMsg": {
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"l3NetworkUuid": "a17f2ea774ba41caadea0b937a7329a3",
"session": {
"uuid": "47bd38c2233d469db97930ab8c71e699"
}
}
}
 *
 * @msg
 * {
"org.zstack.network.securitygroup.APIAttachSecurityGroupToL3NetworkMsg": {
"securityGroupUuid": "3904b4837f0c4f539063777ed463b648",
"l3NetworkUuid": "a17f2ea774ba41caadea0b937a7329a3",
"session": {
"uuid": "47bd38c2233d469db97930ab8c71e699"
},
"timeout": 1800000,
"id": "08b10e1161d04d2694328cc274227226",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIAttachSecurityGroupToL3NetworkEvent`
 */
@Action(category = SecurityGroupConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/security-groups/{securityGroupUuid}/l3-networks/{l3NetworkUuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachSecurityGroupToL3NetworkEvent.class
)
public class APIAttachSecurityGroupToL3NetworkMsg extends APIMessage {
    @APIParam(resourceType=SecurityGroupVO.class, checkAccount = true, operationTarget = true)
    private String securityGroupUuid;
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;
    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }
    public void setSecurityGroupUuid(String securityGroupUuid) {
        this.securityGroupUuid = securityGroupUuid;
    }
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }
    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
 
    public static APIAttachSecurityGroupToL3NetworkMsg __example__() {
        APIAttachSecurityGroupToL3NetworkMsg msg = new APIAttachSecurityGroupToL3NetworkMsg();
        msg.setSecurityGroupUuid(uuid());
        msg.setL3NetworkUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Attached security group[uuid:%s]",securityGroupUuid).resource(l3NetworkUuid,L3NetworkVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
