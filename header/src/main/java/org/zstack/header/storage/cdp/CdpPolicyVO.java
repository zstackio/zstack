package org.zstack.header.storage.cdp;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = CdpPolicyEO.class)
@BaseResource

public class CdpPolicyVO extends CdpPolicyAO {

    public CdpPolicyVO() {
    }

    protected CdpPolicyVO(CdpPolicyVO vo) {
        this.setUuid(vo.getUuid());
        this.setCdpPolicyName(vo.getCdpPolicyName());
        this.setCdpPolicyDescription(vo.getCdpPolicyDescription());
        this.setCdpRetentionTime(vo.getCdpRetentionTime());
        this.setCdpIncrementalPointInMinutes(vo.getCdpIncrementalPointInMinutes());
        this.setCdpRecoveryPointInSeconds(vo.getCdpRecoveryPointInSeconds());
    }

}
