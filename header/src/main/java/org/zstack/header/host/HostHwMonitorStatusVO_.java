package org.zstack.header.host;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/5/28 10:57
 */
@StaticMetamodel(HostHwMonitorStatusVO.class)
public class HostHwMonitorStatusVO_ {

    public static volatile SingularAttribute<HostHwMonitorStatusVO, String> uuid;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, Boolean> cpuStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, Boolean> memoryStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, Boolean> diskStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, Boolean> nicStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, Boolean> gpuStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, Boolean> powerSupplyStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, Boolean> fanStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, Boolean> raidStatus;
    public static volatile SingularAttribute<HostHwMonitorStatusVO, Boolean> temperatureStatus;
}
