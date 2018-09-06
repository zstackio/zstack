package org.zstack.header.identity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(AccountAccessKeyVO.class)
public class AccountAccessKeyVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<AccountAccessKeyVO, String> accountUuid;
    public static volatile SingularAttribute<AccountAccessKeyVO, String> userUuid;
    public static volatile SingularAttribute<AccountAccessKeyVO, String> AccessKeyID;
    public static volatile SingularAttribute<AccountAccessKeyVO, String> AccessKeySecret;
    public static volatile SingularAttribute<AccountAccessKeyVO, String> description;
    public static volatile SingularAttribute<AccountAccessKeyVO, Timestamp> createDate;
    public static volatile SingularAttribute<AccountAccessKeyVO, Timestamp> lastOpDate;
}
