package org.zstack.header.vm;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(TemplateVmInstanceVO.class)
public class TemplateVmInstanceVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<TemplateVmInstanceVO, String> name;
    public static volatile SingularAttribute<TemplateVmInstanceVO, Timestamp> createDate;
    public static volatile SingularAttribute<TemplateVmInstanceVO, Timestamp> lastOpDate;
}
