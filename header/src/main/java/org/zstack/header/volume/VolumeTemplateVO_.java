package org.zstack.header.volume;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import java.sql.Timestamp;

public class VolumeTemplateVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VolumeTemplateVO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeTemplateVO, VolumeType> originalType;
    public static volatile SingularAttribute<VolumeTemplateVO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeTemplateVO, Timestamp> lastOpDate;
}
