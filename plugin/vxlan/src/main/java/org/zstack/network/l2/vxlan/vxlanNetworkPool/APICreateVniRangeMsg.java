package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by weiwang on 09/03/2017.
 */
@Action(category = VxlanNetworkPoolConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l2-networks/vxlan-pool/{l2NetworkUuid}/vni-range",
        method = HttpMethod.POST,
        responseClass = APICreateVniRangeEvent.class,
        parameterName = "params"
)
public class APICreateVniRangeMsg extends APICreateMessage implements L2NetworkMessage{
    @APIParam
    private String uuid;

    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam
    private Integer startVni;

    @APIParam
    private Integer endVni;

    @APIParam
    private String l2NetworkUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStartVni() {
        return startVni;
    }

    public void setStartVni(Integer startVni) {
        this.startVni = startVni;
    }

    public Integer getEndVni() {
        return endVni;
    }

    public void setEndVni(Integer endVni) {
        this.endVni = endVni;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public static APICreateVniRangeMsg __exmaple__() {
        APICreateVniRangeMsg msg = new APICreateVniRangeMsg();

        msg.setName("TestVniRange");
        msg.setDescription("Here is a Vni Range");
        msg.setStartVni(10);
        msg.setEndVni(5000);
        msg.setL2NetworkUuid(uuid());

        return msg;
    }
}
