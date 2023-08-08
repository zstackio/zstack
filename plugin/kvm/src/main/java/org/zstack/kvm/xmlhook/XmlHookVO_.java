package org.zstack.kvm.xmlhook;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(XmlHookVO.class)
public class XmlHookVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<XmlHookVO, String> name;
    public static volatile SingularAttribute<XmlHookVO, String> description;
    public static volatile SingularAttribute<XmlHookVO, XmlHookType> type;
    public static volatile SingularAttribute<XmlHookVO, String> hookScript;
    public static volatile SingularAttribute<XmlHookVO, String> libvirtVersion;
    public static volatile SingularAttribute<XmlHookVO, Timestamp> createDate;
    public static volatile SingularAttribute<XmlHookVO, Timestamp> lastOpDate;
}
