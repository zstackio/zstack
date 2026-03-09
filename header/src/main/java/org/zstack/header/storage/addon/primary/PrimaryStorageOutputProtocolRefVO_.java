package org.zstack.header.storage.addon.primary;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(PrimaryStorageOutputProtocolRefVO.class)
public class PrimaryStorageOutputProtocolRefVO_ {
    public static volatile SingularAttribute<PrimaryStorageOutputProtocolRefVO, Long> id;
    public static volatile SingularAttribute<PrimaryStorageOutputProtocolRefVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<PrimaryStorageOutputProtocolRefVO, String> outputProtocol;
}
