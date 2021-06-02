package org.zstack.header.storage.cdp;

import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public class CdpPolicyAO extends ResourceVO {
    @Column
    @Index
    private String name;

    @Column
    private String description;

    @Column
    private Integer retentionTimeInDays;

    @Column
    private Integer incrementalPointInMinutes;

    @Column
    private Integer recoveryPointInSeconds;

    public CdpPolicyAO() {
    }

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

}
