package org.zstack.header.tag;

/**
 */

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(UserTagVO.class)
public class UserTagVO_ extends TagAO_ {
    public static volatile SingularAttribute<UserTagVO_, String> tagPatternUuid;
}
