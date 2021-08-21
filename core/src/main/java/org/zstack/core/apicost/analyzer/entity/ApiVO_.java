package org.zstack.core.apicost.analyzer.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by huaxin on 2021/7/9.
 */
@StaticMetamodel(ApiVO.class)
public class ApiVO_ extends ResourceVO_ {

    public static volatile SingularAttribute<ApiVO, String> name;
    public static volatile SingularAttribute<ApiVO, String> apiId;
    public static volatile SingularAttribute<ApiVO, Timestamp> lastUpdate;

}
