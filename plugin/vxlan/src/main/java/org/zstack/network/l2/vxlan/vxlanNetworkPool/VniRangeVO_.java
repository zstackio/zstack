package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by weiwang on 08/03/2017.
 */
@StaticMetamodel(VniRangeVO.class)
public class VniRangeVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VniRangeVO, String> name;
    public static volatile SingularAttribute<VniRangeVO, String> description;
    public static volatile SingularAttribute<VniRangeVO, Integer> startVni;
    public static volatile SingularAttribute<VniRangeVO, Integer> endVni;
    public static volatile SingularAttribute<VniRangeVO, String> l2NetworkUuid;
}
