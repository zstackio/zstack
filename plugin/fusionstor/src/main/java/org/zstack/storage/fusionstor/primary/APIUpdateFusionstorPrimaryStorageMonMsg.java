package org.zstack.storage.fusionstor.primary;

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
        path = "/primary-storage/fusionstor/mons/{monUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateMonToFusionstorPrimaryStorageEvent.class
)
public class APIUpdateFusionstorPrimaryStorageMonMsg extends APIMessage implements PrimaryStorageMessage {
    @APINoSee
    private String primaryStorageUuid;

    @APIParam(resourceType = FusionstorPrimaryStorageMonVO.class, emptyString = false)
    private String monUuid;

    @APIParam(maxLength = 255, required = false)
    private String hostname;

    @APIParam(maxLength = 255, required = false)
    private String sshUsername;

    @APIParam(maxLength = 255, required = false)
    private String sshPassword;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshPort;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer monPort;


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

    public Integer getMonPort() {
        return monPort;
    }

    public void setMonPort(Integer monPort) {
        this.monPort = monPort;
    }

    public String getMonUuid() {
        return monUuid;
    }

    public void setMonUuid(String monUuid) {
        this.monUuid = monUuid;
    }
}
