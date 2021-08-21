package org.zstack.core.apicost.analyzer.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;

/**
 * Created by huaxin on 2021/7/9.
 */
@StaticMetamodel(StepLogVO.class)
public class StepLogVO_ extends ResourceVO_ {

    public static volatile SingularAttribute<StepLogVO, String> name;
    public static volatile SingularAttribute<StepLogVO, String> stepId;
    public static volatile SingularAttribute<StepLogVO, String> apiLogId;
    public static volatile SingularAttribute<StepLogVO, Long> startTime;
    public static volatile SingularAttribute<StepLogVO, Long> endTime;
    public static volatile SingularAttribute<StepLogVO, BigDecimal> wait;

}
