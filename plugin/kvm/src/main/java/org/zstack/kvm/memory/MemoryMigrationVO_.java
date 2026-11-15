package org.zstack.kvm.memory;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(MemoryMigrationVO.class)
public class MemoryMigrationVO_ {
    public static volatile SingularAttribute<MemoryMigrationVO, String> vmUuid;
    public static volatile SingularAttribute<MemoryMigrationVO, String> operationUuid;
    public static volatile SingularAttribute<MemoryMigrationVO, String> sourceHostUuid;
    public static volatile SingularAttribute<MemoryMigrationVO, String> targetHostUuid;
    public static volatile SingularAttribute<MemoryMigrationVO, String> poolGeneration;
    public static volatile SingularAttribute<MemoryMigrationVO, String> targetPoolGeneration;
    public static volatile SingularAttribute<MemoryMigrationVO, String> sourceHoldStatus;
    public static volatile SingularAttribute<MemoryMigrationVO, String> targetHoldStatus;
    public static volatile SingularAttribute<MemoryMigrationVO, Boolean> swapDirect;
    public static volatile SingularAttribute<MemoryMigrationVO, String> swapDirectState;
    public static volatile SingularAttribute<MemoryMigrationVO, String> sourceInstanceGeneration;
    public static volatile SingularAttribute<MemoryMigrationVO, String> targetInstanceGeneration;
    public static volatile SingularAttribute<MemoryMigrationVO, String> status;
    public static volatile SingularAttribute<MemoryMigrationVO, String> reason;
}
