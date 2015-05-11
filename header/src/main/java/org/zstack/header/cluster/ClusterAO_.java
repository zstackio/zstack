package org.zstack.header.cluster;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;
import java.util.Date;

/**
 */
@StaticMetamodel(ClusterAO.class)
public class ClusterAO_ {
    public static volatile SingularAttribute<ClusterAO, String> zoneUuid;
    public static volatile SingularAttribute<ClusterAO, String> description;
    public static volatile SingularAttribute<ClusterAO, String> name;
    public static volatile SingularAttribute<ClusterAO, ClusterState> state;
    public static volatile SingularAttribute<ClusterAO, String> hypervisorType;
    public static volatile SingularAttribute<ClusterAO, String> type;
    public static volatile SingularAttribute<ClusterAO, String> uuid;
    public static volatile SingularAttribute<ClusterAO, String> managementNodeId;
    public static volatile SingularAttribute<ClusterAO, Timestamp> createDate;
    public static volatile SingularAttribute<ClusterAO, Timestamp> lastOpDate;
}
