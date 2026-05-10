package org.zstack.header.server;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PhysicalServerHardwareInfoVO.class)
public class PhysicalServerHardwareInfoVO_ {
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, String> serverUuid;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, String> manufacturer;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, String> model;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, String> serialNumber;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, String> biosVersion;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, String> cpuModel;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Integer> cpuSockets;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Integer> cpuCores;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, String> cpuArchitecture;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Long> totalMemoryBytes;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Integer> memoryModuleCount;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Long> totalDiskBytes;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Integer> diskCount;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Integer> nicCount;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Integer> gpuCount;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, String> healthStatus;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, String> discoverSource;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Timestamp> lastDiscoverDate;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalServerHardwareInfoVO, Timestamp> lastOpDate;
}
