package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;


@Action(category = SdnControllerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/sdn-controllers/{sdnControllerUuid}/hosts/{hostUuid}",
        method = HttpMethod.DELETE,
        responseClass = APISdnControllerRemoveHostEvent.class
)
public class APISdnControllerRemoveHostMsg extends APIMessage implements SdnControllerMessage {
    /**
     * @desc l2Network uuid
     */
    @APIParam(resourceType = SdnControllerVO.class, checkAccount = true, operationTarget = true)
    private String sdnControllerUuid;
    /**
     * @desc cluster uuid. See :ref:`ClusterInventory`
     */
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    /**
     * @desc vSwitch type
     */
    @APIParam(required = false, validValues = {"OvnDpdk", "OvnKernel"})
    private String vSwitchType = "OvnDpdk";

    @Override
    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getvSwitchType() {
        return vSwitchType;
    }

    public void setvSwitchType(String vSwitchType) {
        this.vSwitchType = vSwitchType;
    }

    public static APISdnControllerRemoveHostMsg __example__() {
        APISdnControllerRemoveHostMsg msg = new APISdnControllerRemoveHostMsg();

        msg.setSdnControllerUuid(uuid());
        msg.setHostUuid(uuid());
        msg.setvSwitchType("OvnDpdk");

        return msg;
    }
}
