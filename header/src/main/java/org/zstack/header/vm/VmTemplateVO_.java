package org.zstack.header.vm;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import java.sql.Timestamp;

public class VmTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmTemplateVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VmTemplateVO, String> zoneUuid;
    public static volatile SingularAttribute<VmTemplateVO, String> originalType;
    public static volatile SingularAttribute<VmTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmTemplateVO, Timestamp> lastOpDate;
}
