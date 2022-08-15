package org.zstack.sdnController.header;

import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by shixin.ruan on 09/20/2019.
 */
@StaticMetamodel(VxlanNetworkPoolVO.class)
public class HardwareL2VxlanNetworkPoolVO_ extends L2NetworkVO_ {
    public static volatile SingularAttribute<VxlanNetworkPoolVO, String> sdnControllerUuid;
}
