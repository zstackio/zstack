package org.zstack.network.l2.vxlan.vtep;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by weiwang on 02/03/2017.
 */
@StaticMetamodel(VtepVO.class)
public class VtepVO_ {
   public static volatile SingularAttribute<VtepVO, String> uuid;
   public static volatile SingularAttribute<VtepVO, String> hostUuid;
   public static volatile SingularAttribute<VtepVO, String> vtepIp;
   public static volatile SingularAttribute<VtepVO, Integer> port;
   public static volatile SingularAttribute<VtepVO, String> physicalInterface;
   public static volatile SingularAttribute<VtepVO, String> vtepCidr;
   public static volatile SingularAttribute<VtepVO, String> clusterUuid;

}
