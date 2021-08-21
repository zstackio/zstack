package org.zstack.core.apicost.analyzer.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;

/**
 * Created by huaxin on 2021/7/9.
 */
@StaticMetamodel(StepVO.class)
public class StepVO_ extends ResourceVO_ {

    public static volatile SingularAttribute<ApiVO, String> name;
    public static volatile SingularAttribute<ApiVO, String> apiId;
    public static volatile SingularAttribute<ApiVO, String> stepId;
    public static volatile SingularAttribute<ApiVO, BigDecimal> meanWait;
    public static volatile SingularAttribute<ApiVO, Integer> logCount;

}
