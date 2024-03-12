package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by boce.wang on 03/20/2024.
 */
@RestRequest(
        path = "/l2-networks/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeL2NetworkVlanIdEvent.class,
        isAction = true
)
@Action(category = L2NetworkConstant.ACTION_CATEGORY)
public class APIChangeL2NetworkVlanIdMsg extends APIMessage implements L2NetworkMessage, APIAuditor {
    @APIParam(resourceType = L2NetworkVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(required = false)
    private Integer vlan;
    @APIParam(required = false)
    private String type;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getVlan() {
        return vlan;
    }

    public void setVlan(Integer vlan) {
        this.vlan = vlan;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getL2NetworkUuid() {
        return uuid;
    }
    public static APIChangeL2NetworkVlanIdMsg __example__() {
        APIChangeL2NetworkVlanIdMsg msg = new APIChangeL2NetworkVlanIdMsg();
        msg.setUuid(uuid());
        msg.setVlan(1);
        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(rsp.isSuccess() ? ((APIChangeL2NetworkVlanIdEvent)rsp).getInventory().getUuid() : "", L2NetworkVO.class);
    }
}
