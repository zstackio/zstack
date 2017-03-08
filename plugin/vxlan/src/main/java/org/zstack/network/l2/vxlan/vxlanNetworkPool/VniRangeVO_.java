package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import javax.persistence.metamodel.SingularAttribute;

/**
 * Created by weiwang on 08/03/2017.
 */
public class VniRangeVO_ {
    public static volatile SingularAttribute<VniRangeVO, String> uuid;
    public static volatile SingularAttribute<VniRangeVO, String> name;
    public static volatile SingularAttribute<VniRangeVO, String> description;
    public static volatile SingularAttribute<VniRangeVO, String> startVni;
    public static volatile SingularAttribute<VniRangeVO, Integer> endVni;
    public static volatile SingularAttribute<VniRangeVO, Integer> poolUuid;
}
