package org.zstack.sdnController.header;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by boce.wang on 06/16/2025.
 */
@StaticMetamodel(H3cSdnControllerTenantVO.class)
public class H3cSdnControllerTenantVO_ {
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, String> uuid;
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, String> sdnControllerUuid;
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, String> tenantUuid;
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, String> vdsUuid;
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, String> tenantName;
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, String> vdsName;
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, String> cloudDomainName;
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, String> state;
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, Timestamp> createDate;
    public static volatile SingularAttribute<H3cSdnControllerTenantVO, Timestamp> lastOpDate;
}
