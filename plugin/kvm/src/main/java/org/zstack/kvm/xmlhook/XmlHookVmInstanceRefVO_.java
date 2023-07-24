package org.zstack.kvm.xmlhook;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(XmlHookVmInstanceRefVO.class)
public class XmlHookVmInstanceRefVO_ {
    public static volatile SingularAttribute<XmlHookVmInstanceRefVO, Long> id;
    public static volatile SingularAttribute<XmlHookVmInstanceRefVO, String> xmlHookUuid;
    public static volatile SingularAttribute<XmlHookVmInstanceRefVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<XmlHookVmInstanceRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<XmlHookVmInstanceRefVO, Timestamp> lastOpDate;
}
