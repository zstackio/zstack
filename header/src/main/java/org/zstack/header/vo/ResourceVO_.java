package org.zstack.header.vo;

/**
 * Created by xing5 on 2017/4/29.
 */

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ResourceVO.class)
public class ResourceVO_ {
    public static volatile SingularAttribute<ResourceVO, String> uuid;
    public static volatile SingularAttribute<ResourceVO, String> resourceName;
    public static volatile SingularAttribute<ResourceVO, String> resourceType;
    public static volatile SingularAttribute<ResourceVO, String> concreteResourceType;
}
