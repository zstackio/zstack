package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.header.network.sdncontroller.SdnControllerMessage;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@Action(category = SdnControllerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/sdn-controllers/{sdnControllerUuid}/hosts/{hostUuid}",
        method = HttpMethod.POST,
        responseClass = APISdnControllerAddHostEvent.class,
        parameterName = "null"
)
public class APISdnControllerAddHostMsg extends APIMessage implements SdnControllerMessage {
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

    /**
     * @desc physical nics used by vSwitchType
     */
    @APIParam(nonempty = true)
    private List<String> nicNames;

    /**
     * @desc VTEP (VXLAN Tunnel End Point) IP address
     */
    @APIParam(required = false)
    private String vtepIp;

    /**
     * @desc Netmask for the VTEP IP address
     */
    @APIParam(required = false)
    private String netmask;

    /**
     * @desc bonding mode
     */
    @APIParam(required = false)
    private String bondMode;

    /**
     * @desc lacp mode
     */
    @APIParam(required = false)
    private String lacpMode;


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

    public List<String> getNicNames() {
        return nicNames;
    }

    public void setNicNames(List<String> nicNames) {
        this.nicNames = nicNames;
    }

    public String getVtepIp() {
        return vtepIp;
    }

    public void setVtepIp(String vtepIp) {
        this.vtepIp = vtepIp;
    }

    public String getBondMode() {
        return bondMode;
    }

    public void setBondMode(String bondMode) {
        this.bondMode = bondMode;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getLacpMode() {
        return lacpMode;
    }

    public void setLacpMode(String lacpMode) {
        this.lacpMode = lacpMode;
    }


    public static APISdnControllerAddHostMsg __example__() {
        APISdnControllerAddHostMsg msg = new APISdnControllerAddHostMsg();

        msg.setSdnControllerUuid(uuid());
        msg.setHostUuid(uuid());
        msg.setvSwitchType(SdnControllerConstant.H3C_VCFC_CONTROLLER);
        msg.setVtepIp("192.168.1.101");
        msg.setNetmask("255.255.255.0");
        msg.setNetmask("ens1 ens2");
        msg.setBondMode(L2NetworkConstant.BONDING_MODE_AB);
        msg.setLacpMode(L2NetworkConstant.LACP_MODE_OFF);

        return msg;
    }
}
