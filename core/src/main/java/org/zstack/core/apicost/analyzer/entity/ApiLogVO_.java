package org.zstack.core.apicost.analyzer.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by huaxin on 2021/7/9.
 */
@StaticMetamodel(ApiLogVO.class)
public class ApiLogVO_ extends ResourceVO_ {

    public static volatile SingularAttribute<ApiLogVO, String> name;
    public static volatile SingularAttribute<ApiLogVO, String> apiId;
    public static volatile SingularAttribute<ApiLogVO, Integer> isAnalyzed;
    public static volatile SingularAttribute<ApiLogVO, String> originApiId;

}
