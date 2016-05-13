package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * @api
 *
 * start a vm instance
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
{
"org.zstack.header.vm.APIStartVmInstanceMsg": {
"uuid": "a57042713a3d4cdbb99c7c98b6628da8",
"session": {
"uuid": "9f08a11275d647b3a96c1c9766d8a4a4"
}
}
}
 *
 * @msg
 *{
"org.zstack.header.vm.APIStartVmInstanceMsg": {
"uuid": "a57042713a3d4cdbb99c7c98b6628da8",
"session": {
"uuid": "9f08a11275d647b3a96c1c9766d8a4a4"
},
"timeout": 1800000,
"id": "c685c27b821649d29added08e7fecdc2",
"serviceId": "api.portal"
}
}
 * @result
 *
 * See :ref:`APIStartVmInstanceEvent`
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIStartVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @APIParam(resourceType = ClusterVO.class, required = false)
    private String clusterUuid;
    @APIParam(resourceType = HostVO.class, required = false)
    private String hostUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }

}
