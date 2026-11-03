package org.zstack.kvm.memory;

import java.sql.Timestamp;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(MemoryTaskVO.class)
public class MemoryTaskVO_ {
    public static volatile SingularAttribute<MemoryTaskVO, String> uuid;
    public static volatile SingularAttribute<MemoryTaskVO, String> hostUuid;
    public static volatile SingularAttribute<MemoryTaskVO, String> actorUuid;
    public static volatile SingularAttribute<MemoryTaskVO, String> action;
    public static volatile SingularAttribute<MemoryTaskVO, String> status;
    public static volatile SingularAttribute<MemoryTaskVO, String> reason;
    public static volatile SingularAttribute<MemoryTaskVO, Long> desiredRevision;
    public static volatile SingularAttribute<MemoryTaskVO, Timestamp> createDate;
    public static volatile SingularAttribute<MemoryTaskVO, Timestamp> lastOpDate;
}
