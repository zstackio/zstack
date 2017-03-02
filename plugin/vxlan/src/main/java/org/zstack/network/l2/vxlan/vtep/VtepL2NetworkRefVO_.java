package org.zstack.network.l2.vxlan.vtep;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by weiwang on 02/03/2017.
 */
@StaticMetamodel(VtepL2NetworkRefVO.class)
public class VtepL2NetworkRefVO_ {
    public static volatile SingularAttribute<VtepL2NetworkRefVO, String> l2NetworkUuid;
    public static volatile SingularAttribute<VtepL2NetworkRefVO, String> vtepUuid;
}
