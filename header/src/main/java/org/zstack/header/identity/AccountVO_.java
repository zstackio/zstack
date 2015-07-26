package org.zstack.header.identity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccountVO.class)
public class AccountVO_ {
    public static volatile SingularAttribute<AccountVO, String> uuid;
    public static volatile SingularAttribute<AccountVO, String> name;
    public static volatile SingularAttribute<AccountVO, String> description;
    public static volatile SingularAttribute<AccountVO, String> password;
    public static volatile SingularAttribute<AccountVO, AccountType> type;
    public static volatile SingularAttribute<AccountVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccountVO, Timestamp> lastOpDate;
}
