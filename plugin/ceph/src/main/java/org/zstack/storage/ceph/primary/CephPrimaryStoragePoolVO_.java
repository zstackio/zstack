package org.zstack.storage.ceph.primary;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/2/28.
 */
@StaticMetamodel(CephPrimaryStoragePoolVO.class)
public class CephPrimaryStoragePoolVO_ {
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, String> uuid;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, String> poolName;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, String> description;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, Timestamp> createDate;
    public static volatile SingularAttribute<CephPrimaryStoragePoolVO, Timestamp> lastOpDate;
}
