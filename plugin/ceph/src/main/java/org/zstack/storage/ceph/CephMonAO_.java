package org.zstack.storage.ceph;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(CephMonAO.class)
public class CephMonAO_ {
    public static volatile SingularAttribute<CephMonAO, String> uuid;
    public static volatile SingularAttribute<CephMonAO, String> sshUsername;
    public static volatile SingularAttribute<CephMonAO, String> sshPassword;
    public static volatile SingularAttribute<CephMonAO, String> sshPort;
    public static volatile SingularAttribute<CephMonAO, String> hostname;
    public static volatile SingularAttribute<CephMonAO, Integer> monPort;
    public static volatile SingularAttribute<CephMonAO, MonStatus> status;
    public static volatile SingularAttribute<CephMonAO, Timestamp> createDate;
    public static volatile SingularAttribute<CephMonAO, Timestamp> lastOpDate;
}
