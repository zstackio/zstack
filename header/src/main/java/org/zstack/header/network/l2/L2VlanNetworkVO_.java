package org.zstack.header.network.l2;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(L2VlanNetworkVO.class)
public class L2VlanNetworkVO_ extends L2NetworkVO_ {
    public static volatile SingularAttribute<L2VlanNetworkVO, Integer> vlan;
}
