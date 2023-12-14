package org.zstack.header.securitymachine.secretresourcepool;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/10/28 11:38
 */
@StaticMetamodel(SecretResourcePoolVO.class)
public class SecretResourcePoolVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SecretResourcePoolVO, String> zoneUuid;
    public static volatile SingularAttribute<SecretResourcePoolVO, String> description;
    public static volatile SingularAttribute<SecretResourcePoolVO, String> name;
    public static volatile SingularAttribute<SecretResourcePoolVO, SecretResourcePoolType> type;
    public static volatile SingularAttribute<SecretResourcePoolVO, SecretResourcePoolState> state;
    public static volatile SingularAttribute<SecretResourcePoolVO, String> model;
    public static volatile SingularAttribute<SecretResourcePoolVO, Integer> heartbeatInterval;
    public static volatile SingularAttribute<SecretResourcePoolVO, Timestamp> createDate;
    public static volatile SingularAttribute<SecretResourcePoolVO, Timestamp> lastOpDate;
}
