package org.zstack.sdnController.header;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by boce.wang on 07/02/2025.
 */
@StaticMetamodel(H3cSdnSubnetIpRangeRefVO.class)
public class H3cSdnSubnetIpRangeRefVO_ {
    public static volatile SingularAttribute<H3cSdnSubnetIpRangeRefVO, String> uuid;
    public static volatile SingularAttribute<H3cSdnSubnetIpRangeRefVO, String> sdnControllerUuid;
    public static volatile SingularAttribute<H3cSdnSubnetIpRangeRefVO, String> ipRangeUuid;
    public static volatile SingularAttribute<H3cSdnSubnetIpRangeRefVO, String> subnetUuid;
    public static volatile SingularAttribute<H3cSdnSubnetIpRangeRefVO, String> l2NetworkUuid;
    public static volatile SingularAttribute<H3cSdnSubnetIpRangeRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<H3cSdnSubnetIpRangeRefVO, Timestamp> lastOpDate;
}
