package org.zstack.storage.surfs.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;

/**
 * Created by Mei Lei on 6/6/2016.
 */
@RestRequest(
        path = "/primary-storage/surfs/nodes/{nodeUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateNodeToSurfsPrimaryStorageEvent.class
)
public class APIUpdateSurfsPrimaryStorageNodeMsg extends APIMessage implements PrimaryStorageMessage {
    @APINoSee
    private String primaryStorageUuid;

    @APIParam(resourceType = SurfsPrimaryStorageNodeVO.class, emptyString = false)
    private String nodeUuid;

    @APIParam(maxLength = 255, required = false)
    private String hostname;

    @APIParam(maxLength = 255, required = false)
    private String sshUsername;

    @APIParam(maxLength = 255, required = false)
    private String sshPassword;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshPort;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer nodePort;


    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }


    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public Integer getNodePort() {
        return nodePort;
    }

    public void setNodePort(Integer nodePort) {
        this.nodePort = nodePort;
    }

    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }
 
    public static APIUpdateSurfsPrimaryStorageNodeMsg __example__() {
        APIUpdateSurfsPrimaryStorageNodeMsg msg = new APIUpdateSurfsPrimaryStorageNodeMsg();
        msg.setNodeUuid(uuid());
        msg.setNodePort(1234);
        return msg;
    }

}
