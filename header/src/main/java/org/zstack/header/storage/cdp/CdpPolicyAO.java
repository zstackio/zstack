package org.zstack.header.storage.cdp;

import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public class CdpPolicyAO extends ResourceVO {
    @Column
    @Index
    private String policyName;

    @Column
    private String policyDescription;

    @Column
    private Integer PreserveTimeInDays;

    @Column
    private Integer bpInMinutes;

    @Column
    private Integer rpInSeconds;

    public CdpPolicyAO() {
    }

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

}
