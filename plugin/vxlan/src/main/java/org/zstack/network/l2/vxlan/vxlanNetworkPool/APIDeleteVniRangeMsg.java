package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkMessage;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 03/05/2017.
 */
@RestRequest(
        path = "/l2-networks/vxlan-pool/vni-ranges/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVniRangeEvent.class
)
public class APIDeleteVniRangeMsg extends APIDeleteMessage implements L2NetworkMessage {
    @APIParam(resourceType = VniRangeVO.class, successIfResourceNotExisting = true,
            checkAccount = true, operationTarget = true)
    private String uuid;

    @APINoSee
    private String l2NetworkUuid;

    public APIDeleteVniRangeMsg(String uuid) {
        super();
        this.uuid = uuid;
    }

    public APIDeleteVniRangeMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public static APIDeleteVniRangeMsg __example__() {
        APIDeleteVniRangeMsg msg = new APIDeleteVniRangeMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
