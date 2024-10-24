package org.zstack.header.host;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;
/**
 * Created by boce.wang on 10/24/2024.
 */
@StaticMetamodel(HostNetworkLabelVO.class)
public class HostNetworkLabelVO_ {
    public static volatile SingularAttribute<HostNetworkLabelVO, String> uuid;
    public static volatile SingularAttribute<HostNetworkLabelVO, String> serviceType;
    public static volatile SingularAttribute<HostNetworkLabelVO, Boolean> system;
    public static volatile SingularAttribute<HostNetworkLabelVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostNetworkLabelVO, Timestamp> lastOpDate;
}
