package org.zstack.sdnController.header;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SdnControllerVO.class)
public class SdnControllerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SdnControllerVO, String> vendorType;
    public static volatile SingularAttribute<SdnControllerVO, String> name;
    public static volatile SingularAttribute<SdnControllerVO, String> description;
    public static volatile SingularAttribute<SdnControllerVO, String> ip;
    public static volatile SingularAttribute<SdnControllerVO, String> username;
    public static volatile SingularAttribute<SdnControllerVO, String> password;
    public static volatile SingularAttribute<SdnControllerVO, Timestamp> createDate;
    public static volatile SingularAttribute<SdnControllerVO, Timestamp> lastOpDate;
}
