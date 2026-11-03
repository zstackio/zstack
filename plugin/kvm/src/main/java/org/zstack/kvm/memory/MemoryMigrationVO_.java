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
    public static volatile SingularAttribute<MemoryMigrationVO, String> sourceInstanceGeneration;
    public static volatile SingularAttribute<MemoryMigrationVO, String> targetInstanceGeneration;
    public static volatile SingularAttribute<MemoryMigrationVO, String> status;
    public static volatile SingularAttribute<MemoryMigrationVO, String> reason;
}
