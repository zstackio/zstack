package org.zstack.sdnController.header;

import org.zstack.header.network.l2.L2NetworkVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by shixin.ruan on 09/20/2019.
 */
@StaticMetamodel(HardwareL2VxlanNetworkPoolVO.class)
public class HardwareL2VxlanNetworkPoolVO_ extends L2NetworkVO_ {
    public static volatile SingularAttribute<HardwareL2VxlanNetworkPoolVO, String> sdnControllerUuid;
}
