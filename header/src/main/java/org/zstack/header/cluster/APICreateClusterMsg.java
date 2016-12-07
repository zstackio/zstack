package org.zstack.header.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

/**
 * @api create a new cluster in zone
 * @cli
 * @httpMsg {
 * "org.zstack.header.cluster.APICreateClusterMsg": {
 * "session": {
 * "uuid": "7d114b56078245dbb85bd72364949220"
 * },
 * "zoneUuid": "1b830f5bd1cb469b821b4b77babfdd6f",
 * "description": "test",
 * "name": "cluster1",
 * "hypervisorType": "KVM"
 * }
 * }
 * @msg {
 * "org.zstack.header.cluster.APICreateClusterMsg": {
 * "zoneUuid": "1b830f5bd1cb469b821b4b77babfdd6f",
 * "name": "cluster1",
 * "description": "test",
 * "hypervisorType": "KVM",
 * "session": {
 * "uuid": "7d114b56078245dbb85bd72364949220"
 * },
 * "timeout": 1800000,
 * "id": "0609791c70954aaf9b0256fb27aeaadc",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateClusterEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/clusters",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APICreateClusterEvent.class
)
public class APICreateClusterMsg extends APICreateMessage {
    /**
     * @desc uuid of zone this cluster is going to create in
     */
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;
    /**
     * @desc zone name, max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc see field 'hypervisorType' of :ref:`ClusterInventory` for details
     * @choices - KVM
     * - Simulator
     */
    @APIParam(validValues = {"KVM", "Simulator"})
    private String hypervisorType;
    /**
     * @desc see field 'type' of :ref:`ClusterInventory` for details
     * @choices zstack
     */
    @APIParam(required = false, validValues = {"zstack"})
    private String type;

    public APICreateClusterMsg() {
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterName() {
        return name;
    }

    public void setClusterName(String clusterName) {
        this.name = clusterName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
