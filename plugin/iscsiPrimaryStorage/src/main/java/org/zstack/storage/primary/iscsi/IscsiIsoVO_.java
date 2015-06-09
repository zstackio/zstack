package org.zstack.storage.primary.iscsi;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 6/8/2015.
 */
@StaticMetamodel(IscsiIsoVO.class)
public class IscsiIsoVO_ {
    public static volatile SingularAttribute<IscsiIsoVO, String> uuid;
    public static volatile SingularAttribute<IscsiIsoVO, String> imageUuid;
    public static volatile SingularAttribute<IscsiIsoVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<IscsiIsoVO, String> target;
    public static volatile SingularAttribute<IscsiIsoVO, Integer> lun;
    public static volatile SingularAttribute<IscsiIsoVO, String> hostname;
    public static volatile SingularAttribute<IscsiIsoVO, Integer> port;
    public static volatile SingularAttribute<IscsiIsoVO, String> path;
    public static volatile SingularAttribute<IscsiIsoVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<IscsiIsoVO, Timestamp> createDate;
    public static volatile SingularAttribute<IscsiIsoVO, Timestamp> lastOpDate;
}
