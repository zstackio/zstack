package org.zstack.kvm.memory;

import javax.persistence.*;

/** Sticky migration policy, not a transient CPU/PSI/telemetry decision. */
@Entity @Table
public class MemoryVmExclusionVO {
    @Id @Column(length = 32) public String vmUuid;
    @Column(nullable = false, length = 32) public String sourceHostUuid;
    @Column(nullable = false, length = 32) public String targetHostUuid;
    @Column(nullable = false, columnDefinition = "TEXT") public String sourceRevisions;
    @Column(length = 128) public String sourceInstanceGeneration;
    @Column(nullable = false) public long vmPolicyRevision;
    @Column(nullable = false) public boolean retained;

    public MemoryVmExclusionInventory toInventory() {
        MemoryVmExclusionInventory out = new MemoryVmExclusionInventory();
        out.setSourceHostUuid(sourceHostUuid);
        out.setTargetHostUuid(targetHostUuid);
        out.setSourceRevisions(sourceRevisions);
        out.setSourceInstanceGeneration(sourceInstanceGeneration);
        out.setRetained(retained);
        return out;
    }
}
