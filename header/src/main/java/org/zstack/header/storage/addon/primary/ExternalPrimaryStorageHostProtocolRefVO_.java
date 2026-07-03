package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageHostStatus;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ExternalPrimaryStorageHostProtocolRefVO.class)
public class ExternalPrimaryStorageHostProtocolRefVO_ {
    public static volatile SingularAttribute<ExternalPrimaryStorageHostProtocolRefVO, Long> id;
    public static volatile SingularAttribute<ExternalPrimaryStorageHostProtocolRefVO, String> hostUuid;
    public static volatile SingularAttribute<ExternalPrimaryStorageHostProtocolRefVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<ExternalPrimaryStorageHostProtocolRefVO, String> protocol;
    public static volatile SingularAttribute<ExternalPrimaryStorageHostProtocolRefVO, PrimaryStorageHostStatus> status;
    public static volatile SingularAttribute<ExternalPrimaryStorageHostProtocolRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<ExternalPrimaryStorageHostProtocolRefVO, Timestamp> lastOpDate;
}
