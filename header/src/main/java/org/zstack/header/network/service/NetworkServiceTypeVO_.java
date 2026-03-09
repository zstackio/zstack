package org.zstack.header.network.service;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(NetworkServiceTypeVO.class)
public class NetworkServiceTypeVO_ {
    public static volatile SingularAttribute<NetworkServiceTypeVO, Long> id;
    public static volatile SingularAttribute<NetworkServiceTypeVO, String> networkServiceProviderUuid;
    public static volatile SingularAttribute<NetworkServiceTypeVO, String> type;
}
