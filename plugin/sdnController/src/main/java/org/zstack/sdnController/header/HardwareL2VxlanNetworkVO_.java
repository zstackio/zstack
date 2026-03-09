package org.zstack.sdnController.header;

import org.zstack.header.network.l2.L2NetworkVO_;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Created by shixin.ruan on 09/20/2019.
 */
@StaticMetamodel(HardwareL2VxlanNetworkVO.class)
public class HardwareL2VxlanNetworkVO_ extends VxlanNetworkVO_ {
    public static volatile SingularAttribute<HardwareL2VxlanNetworkVO, Integer> vlan;
}
