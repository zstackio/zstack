package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddHostEvent.class
)
public abstract class APIAddHostMsg extends APICreateMessage implements AddHostMessage {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc IPv4 address or DNS name of management nic
     */
    @APIParam(emptyString = false)
    private String managementIp;
    /**
     * @desc uuid of cluster this host belongs to
     */
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public APIAddHostMsg() {
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

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
}
