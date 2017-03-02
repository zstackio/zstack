package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by weiwang on 02/03/2017.
 */
@StaticMetamodel(VxlanNetworkVO.class)
public class VxlanNetworkPoolVO_ extends L2NetworkVO_ {
    public static volatile SingularAttribute<VxlanNetworkPoolVO, Integer> startVni;
    public static volatile SingularAttribute<VxlanNetworkPoolVO, Integer> endVni;
    public static volatile SingularAttribute<VxlanNetworkPoolVO, String> vtepCidr;
}
