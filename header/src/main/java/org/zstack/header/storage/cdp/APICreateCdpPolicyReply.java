package org.zstack.header.storage.cdp;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(allTo = "inventory")
public class APICreateCdpPolicyReply extends APIReply {
    private String policyName;
    private String policyDescription;
    private Integer PreserveTimeInDays;
    private Integer bpInMinutes;
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

    public static APICreateCdpPolicyReply __example__() {
        APICreateCdpPolicyReply reply = new APICreateCdpPolicyReply();

        reply.setCdpPolicyName("MyCdpPolicyName");
        reply.setPolicyDescription("MyCdpDescription");
        reply.setCdpPreserveTime(7);
        reply.setCdpBpInMinutes(60);
        reply.setCdpRpInSeconds(5);

        return reply;
    }

}
