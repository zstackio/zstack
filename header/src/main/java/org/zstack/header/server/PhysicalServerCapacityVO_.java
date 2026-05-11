package org.zstack.header.server;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerCapacityVO.class)
public class PhysicalServerCapacityVO_ {
    public static volatile SingularAttribute<PhysicalServerCapacityVO, String> uuid;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> totalMemory;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> totalCpu;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> cpuNum;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Integer> cpuSockets;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Integer> cpuCoreNum;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> availableMemory;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> availableCpu;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> totalPhysicalMemory;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> availablePhysicalMemory;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Float> cpuOverprovisioningRatio;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Float> memoryOverprovisioningRatio;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> reservedMemory;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> totalDisk;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Long> availableDisk;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, PhysicalServerCapacityState> capacityState;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerCapacityVO, Timestamp> lastOpDate;
}
