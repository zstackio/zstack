package org.zstack.storage.surfs;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by zhouhaiping 2017-08-25
 */
@StaticMetamodel(SurfsNodeAO.class)
public class SurfsNodeAO_ {
    public static volatile SingularAttribute<SurfsNodeAO, String> uuid;
    public static volatile SingularAttribute<SurfsNodeAO, String> sshUsername;
    public static volatile SingularAttribute<SurfsNodeAO, String> sshPassword;
    public static volatile SingularAttribute<SurfsNodeAO, String> hostname;
    public static volatile SingularAttribute<SurfsNodeAO, Integer> nodePort;
    public static volatile SingularAttribute<SurfsNodeAO, NodeStatus> status;
    public static volatile SingularAttribute<SurfsNodeAO, Timestamp> createDate;
    public static volatile SingularAttribute<SurfsNodeAO, Timestamp> lastOpDate;
}
