package org.zstack.header.identity;

import org.zstack.header.vo.ResourceVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccountVO.class)
public class AccountVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AccountVO, String> name;
    public static volatile SingularAttribute<AccountVO, String> description;
    public static volatile SingularAttribute<AccountVO, String> password;
    public static volatile SingularAttribute<AccountVO, AccountType> type;
    public static volatile SingularAttribute<AccountVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccountVO, Timestamp> lastOpDate;
}
