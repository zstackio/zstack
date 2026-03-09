package org.zstack.header.network.sdncontroller;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SdnControllerHostRefVO.class)
public class SdnControllerHostRefVO_ {
    public static volatile SingularAttribute<SdnControllerHostRefVO, Long> id;
    public static volatile SingularAttribute<SdnControllerHostRefVO, String> hostUuid;
    public static volatile SingularAttribute<SdnControllerHostRefVO, String> sdnControllerUuid;
    public static volatile SingularAttribute<SdnControllerHostRefVO, String> vSwitchType;
    public static volatile SingularAttribute<SdnControllerHostRefVO, String> vtepIp;
    public static volatile SingularAttribute<SdnControllerHostRefVO, String> netmask;
    public static volatile SingularAttribute<SdnControllerHostRefVO, String> nicPciAddresses;
    public static volatile SingularAttribute<SdnControllerHostRefVO, String> nicDrivers;
    public static volatile SingularAttribute<SdnControllerHostRefVO, String> bondMode;
    public static volatile SingularAttribute<SdnControllerHostRefVO, String> lacpMode;
}