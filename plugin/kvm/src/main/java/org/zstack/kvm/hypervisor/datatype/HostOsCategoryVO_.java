package org.zstack.kvm.hypervisor.datatype;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostOsCategoryVO.class)
public class HostOsCategoryVO_ {
    public static volatile SingularAttribute<HostOsCategoryVO, String> uuid;
    public static volatile SingularAttribute<HostOsCategoryVO, String> architecture;
    public static volatile SingularAttribute<HostOsCategoryVO, String> osReleaseVersion;
    public static volatile SingularAttribute<HostOsCategoryVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostOsCategoryVO, Timestamp> lastOpDate;
}
