package org.zstack.header.storage.cdp;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
public class APICreateCdpPolicyReply extends APIReply {
    private String name;
    private String description;
    private Integer retentionTimeInDays;
    private Integer incrementalPointInMinutes;
    private Integer recoveryPointInSeconds;

    public void setCdpPolicyName(String name) {
        this.name = name;
    }
    public String getCdpPolicyName() {
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

    public static APICreateCdpPolicyReply __example__() {
        APICreateCdpPolicyReply reply = new APICreateCdpPolicyReply();

        reply.setCdpPolicyName("MyCdpPolicyName");
        reply.setCdpPolicyDescription("MyCdpDescription");
        reply.setCdpRetentionTime(7);
        reply.setCdpIncrementalPointInMinutes(60);
        reply.setCdpRecoveryPointInSeconds(5);

        return reply;
    }

}
