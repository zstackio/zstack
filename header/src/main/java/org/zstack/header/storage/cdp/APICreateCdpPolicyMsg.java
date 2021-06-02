package org.zstack.header.storage.cdp;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.TagResourceType;

@RestRequest(
        path = "/cdp-backup-storage/policy",
        method = HttpMethod.POST,
        responseClass = APICreateCdpPolicyMsg.class,
        parameterName = "params"
)
@TagResourceType(BackupStorageVO.class)
public class APICreateCdpPolicyMsg extends APICreateMessage {

    public String getType() {
        return "CdpBackupStoragePolicy";
    }

    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 255)
    private String description;

    @APIParam(numberRange = {1,30})
    private Integer retentionTimeInDays;

    @APIParam(numberRange = {1,60})
    private Integer incrementalPointInMinutes;

    @APIParam(numberRange = {1,5})
    private Integer recoveryPointInSeconds;

    public void setCdpPolicyName(String name) {
        this.name = name;
    }
    public String getCdpPolicyNameName() {
        return name;
    }

    public void setCdpPolicyDescription(String description) {this.description = description; }
    public String getCdpPolicyDescription() {
        return description;
    }

    public void setCdpRetentionTime(Integer retentionTimeInDays) {
        this.retentionTimeInDays = retentionTimeInDays;
    }
    public Integer getCdpRetentionTime() {
        return retentionTimeInDays;
    }

    public void setCdpIncrementalPointInMinutes(Integer incrementalPointInMinutes) {
        this.incrementalPointInMinutes = incrementalPointInMinutes;
    }
    public Integer getCdpIncrementalPointInMinutes() {
        return incrementalPointInMinutes;
    }

    public void setCdpRecoveryPointInSeconds(Integer recoveryPointInSeconds) {
        this.recoveryPointInSeconds = recoveryPointInSeconds;
    }
    public Integer getCdpRecoveryPointInSeconds() {
        return recoveryPointInSeconds;
    }

    public static org.zstack.header.storage.cdp.APICreateCdpPolicyMsg __example__() {
        org.zstack.header.storage.cdp.APICreateCdpPolicyMsg msg = new org.zstack.header.storage.cdp.APICreateCdpPolicyMsg();

        msg.setCdpPolicyName("MyCdpPolicyName");
        msg.setCdpPolicyDescription("MyCdpDescription");
        msg.setCdpRetentionTime(7);
        msg.setCdpIncrementalPointInMinutes(60);
        msg.setCdpRecoveryPointInSeconds(5);

        return msg;
    }

}