package org.zstack.core.apicost.analyzer.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;

/**
 * Created by huaxin on 2021/7/9.
 */
@StaticMetamodel(RStepVO.class)
public class RStepVO_ extends ResourceVO_ {

    public static volatile SingularAttribute<RStepVO, String> apiId;
    public static volatile SingularAttribute<RStepVO, String> fromStepId;
    public static volatile SingularAttribute<RStepVO, String> toStepId;
    public static volatile SingularAttribute<RStepVO, BigDecimal> weight;
}
