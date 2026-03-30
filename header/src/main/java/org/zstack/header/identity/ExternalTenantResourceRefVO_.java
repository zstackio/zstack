package org.zstack.header.identity;

import java.sql.Timestamp;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * JPA Metamodel for ExternalTenantResourceRefVO
 */
@StaticMetamodel(ExternalTenantResourceRefVO.class)
public class ExternalTenantResourceRefVO_ {
    public static volatile SingularAttribute<ExternalTenantResourceRefVO, Long> id;
    public static volatile SingularAttribute<ExternalTenantResourceRefVO, String> source;
    public static volatile SingularAttribute<ExternalTenantResourceRefVO, String> tenantId;
    public static volatile SingularAttribute<ExternalTenantResourceRefVO, String> userId;
    public static volatile SingularAttribute<ExternalTenantResourceRefVO, String> resourceUuid;
    public static volatile SingularAttribute<ExternalTenantResourceRefVO, String> accountUuid;
    public static volatile SingularAttribute<ExternalTenantResourceRefVO, String> resourceType;
    public static volatile SingularAttribute<ExternalTenantResourceRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<ExternalTenantResourceRefVO, Timestamp> lastOpDate;
}
