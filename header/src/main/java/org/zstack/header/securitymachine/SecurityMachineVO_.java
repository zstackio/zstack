package org.zstack.header.securitymachine;

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolType;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/10/28 11:58
 */
@StaticMetamodel(SecurityMachineVO.class)
public class SecurityMachineVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SecurityMachineVO, String> zoneUuid;
    public static volatile SingularAttribute<SecurityMachineVO, String> name;
    public static volatile SingularAttribute<SecurityMachineVO, String> secretResourcePoolUuid;
    public static volatile SingularAttribute<SecurityMachineVO, String> description;
    public static volatile SingularAttribute<SecurityMachineVO, String> managementIp;
    public static volatile SingularAttribute<SecurityMachineVO, SecurityMachineState> state;
    public static volatile SingularAttribute<SecurityMachineVO, SecurityMachineStatus> status;
    public static volatile SingularAttribute<SecurityMachineVO, SecretResourcePoolType> type;
    public static volatile SingularAttribute<SecurityMachineVO, String> model;
    public static volatile SingularAttribute<SecurityMachineVO, Timestamp> createDate;
    public static volatile SingularAttribute<SecurityMachineVO, Timestamp> lastOpDate;
}