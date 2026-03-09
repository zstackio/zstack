package org.zstack.header.network.service;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(NetworkServiceL3NetworkRefVO.class)
public class NetworkServiceL3NetworkRefVO_ {
    public static volatile SingularAttribute<NetworkServiceL3NetworkRefVO, Long> id;
    public static volatile SingularAttribute<NetworkServiceL3NetworkRefVO, String> l3NetworkUuid;
    public static volatile SingularAttribute<NetworkServiceL3NetworkRefVO, String> networkServiceProviderUuid;
    public static volatile SingularAttribute<NetworkServiceL3NetworkRefVO, String> networkServiceType;
}
