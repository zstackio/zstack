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
    private String policyName;

    @APIParam(maxLength = 255)
    private String policyDescription;

    @APIParam(maxLength = 3, numberRange = {1,30})
    private Integer PreserveTimeInDays;

    @APIParam(maxLength = 3, numberRange = {1,60})
    private Integer bpInMinutes;

    @APIParam(maxLength = 3, numberRange = {1,5})
    private Integer rpInSeconds;

    public void setCdpPolicyName(String policyName) {
        this.policyName = policyName;
    }
    public String getCdpPolicyNameName() {
        return policyName;
    }

    public void setPolicyDescription(String policyDescription) {this.policyDescription = policyDescription; }
    public String getPolicyDescription() {
        return policyDescription;
    }

    public void setCdpPreserveTime(Integer PreserveTimeInDays) {
        this.PreserveTimeInDays = PreserveTimeInDays;
    }
    public Integer getCdpPreserveTime() {
        return PreserveTimeInDays;
    }

    public void setCdpBpInMinutes(Integer bpInMinutes) {
        this.bpInMinutes = bpInMinutes;
    }
    public Integer getCdpBpInMinutes() {
        return bpInMinutes;
    }

    public void setCdpRpInSeconds(Integer rpInSeconds) {
        this.rpInSeconds = rpInSeconds;
    }
    public Integer getCdpRpInSeconds() {
        return rpInSeconds;
    }

    public static org.zstack.header.storage.cdp.APICreateCdpPolicyMsg __example__() {
        org.zstack.header.storage.cdp.APICreateCdpPolicyMsg msg = new org.zstack.header.storage.cdp.APICreateCdpPolicyMsg();

        msg.setCdpPolicyName("MyCdpPolicyName");
        msg.setPolicyDescription("MyCdpDescription");
        msg.setCdpPreserveTime(7);
        msg.setCdpBpInMinutes(60);
        msg.setCdpRpInSeconds(5);

        return msg;
    }

}