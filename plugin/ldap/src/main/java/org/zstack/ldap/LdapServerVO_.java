package org.zstack.ldap;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(LdapServerVO.class)
public class LdapServerVO_ {
    public static volatile SingularAttribute<LdapServerVO, String> uuid;
    public static volatile SingularAttribute<LdapServerVO, String> name;
    public static volatile SingularAttribute<LdapServerVO, String> description;
    public static volatile SingularAttribute<LdapServerVO, String> url;
    public static volatile SingularAttribute<LdapServerVO, String> base;
    public static volatile SingularAttribute<LdapServerVO, String> user;
    public static volatile SingularAttribute<LdapServerVO, String> password;
    public static volatile SingularAttribute<LdapServerVO, String> secure;
    public static volatile SingularAttribute<LdapServerVO, Timestamp> createDate;
    public static volatile SingularAttribute<LdapServerVO, Timestamp> lastOpDate;
}
