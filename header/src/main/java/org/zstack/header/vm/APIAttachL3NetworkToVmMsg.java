package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

/**
 * @api attach a nic to vm. If vm is running, user is responsible for running DHCP client software inside
 * vm in order to get ip address
 * @cli
 * @httpMsg {
 * "org.zstack.header.vm.APIAttachNicToVmMsg": {
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "l3NetworkUuid": "e438b93332ba40dcbb5d553c749a43ca",
 * "session": {
 * "uuid": "f6bcff806cb84ae5b91589ae6716b200"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.vm.APIAttachNicToVmMsg": {
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "l3NetworkUuid": "e438b93332ba40dcbb5d553c749a43ca",
 * "session": {
 * "uuid": "f6bcff806cb84ae5b91589ae6716b200"
 * },
 * "timeout": 1800000,
 * "id": "5a441ef9697c4e7b98c9e86715e4987f",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIAttachNicToVmEvent`
 * @since 0.1.0
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/l3-networks/{l3NetworkUuid}",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APIAttachL3NetworkToVmEvent.class
)
public class APIAttachL3NetworkToVmMsg extends APIMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;
    /**
     * @desc uuid of L3Network where the nic will be created
     */
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true)
    private String l3NetworkUuid;

    private String staticIp;

    public String getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(String staticIp) {
        this.staticIp = staticIp;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
}
