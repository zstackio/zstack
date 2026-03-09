package org.zstack.ldap;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(LdapAccountRefVO.class)
public class LdapAccountRefVO_ {
    public static volatile SingularAttribute<LdapServerVO, String> uuid;
    public static volatile SingularAttribute<LdapServerVO, String> ldapUid;
    public static volatile SingularAttribute<LdapServerVO, String> ldapServerUuid;
    public static volatile SingularAttribute<LdapServerVO, String> accountUuid;
    public static volatile SingularAttribute<LdapServerVO, Timestamp> createDate;
    public static volatile SingularAttribute<LdapServerVO, Timestamp> lastOpDate;
}
