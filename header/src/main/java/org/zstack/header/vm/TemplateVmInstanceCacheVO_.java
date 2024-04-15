package org.zstack.header.vm;


import org.zstack.header.allocator.HostCapacityVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(TemplateVmInstanceCacheVO.class)
public class TemplateVmInstanceCacheVO_ {
    public static volatile SingularAttribute<HostCapacityVO, Long> id;
    public static volatile SingularAttribute<HostCapacityVO, String> templateVmInstanceUuid;
    public static volatile SingularAttribute<HostCapacityVO, String> cacheVmInstanceUuid;
}
