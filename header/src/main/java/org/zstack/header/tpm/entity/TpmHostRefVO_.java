package org.zstack.header.tpm.entity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(TpmHostRefVO.class)
public class TpmHostRefVO_ {
    public static volatile SingularAttribute<TpmHostRefVO, Long> id;
    public static volatile SingularAttribute<TpmHostRefVO, String> tpmUuid;
    public static volatile SingularAttribute<TpmHostRefVO, String> hostUuid;
    public static volatile SingularAttribute<TpmHostRefVO, String> path;
    public static volatile SingularAttribute<TpmHostRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<TpmHostRefVO, Timestamp> lastOpDate;
}
