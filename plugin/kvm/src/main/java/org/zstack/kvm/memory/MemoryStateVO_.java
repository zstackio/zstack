package org.zstack.kvm.memory;

import java.sql.Timestamp;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(MemoryStateVO.class)
public class MemoryStateVO_ {
    public static volatile SingularAttribute<MemoryStateVO, String> hostUuid;
    public static volatile SingularAttribute<MemoryStateVO, Long> desiredRevision;
    public static volatile SingularAttribute<MemoryStateVO, Long> appliedRevision;
    public static volatile SingularAttribute<MemoryStateVO, String> control;
    public static volatile SingularAttribute<MemoryStateVO, String> desired;
    public static volatile SingularAttribute<MemoryStateVO, String> status;
    public static volatile SingularAttribute<MemoryStateVO, String> reason;
    public static volatile SingularAttribute<MemoryStateVO, Integer> failures;
    public static volatile SingularAttribute<MemoryStateVO, Long> nextAttemptAt;
    public static volatile SingularAttribute<MemoryStateVO, String> capabilities;
    public static volatile SingularAttribute<MemoryStateVO, String> state;
    public static volatile SingularAttribute<MemoryStateVO, Long> lastSampleTime;
    public static volatile SingularAttribute<MemoryStateVO, Timestamp> createDate;
    public static volatile SingularAttribute<MemoryStateVO, Timestamp> lastOpDate;
}
