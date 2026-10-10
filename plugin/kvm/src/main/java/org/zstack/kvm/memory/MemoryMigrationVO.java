package org.zstack.kvm.memory;

import javax.persistence.*;

/** Migration control intent, separate from administrator desired policies. */
@Entity @Table
public class MemoryMigrationVO {
    @Id @Column(length = 32) public String vmUuid;
    @Column(nullable = false, length = 32) public String operationUuid;
    @Column(nullable = false, length = 32) public String sourceHostUuid;
    @Column(nullable = false, length = 32) public String targetHostUuid;
    @Column(length = 128) public String poolGeneration;
    @Column(length = 128) public String sourceInstanceGeneration;
    @Column(length = 128) public String targetInstanceGeneration;
    @Column(nullable = false, length = 32) public String status;
    @Column(length = 2048) public String reason;
}
