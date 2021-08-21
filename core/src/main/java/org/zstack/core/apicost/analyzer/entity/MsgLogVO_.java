package org.zstack.core.apicost.analyzer.entity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;

/**
 * Created by huaxin on 2021/7/15.
 */
@StaticMetamodel(MsgLogVO.class)
public class MsgLogVO_ extends ResourceVO_ {

    public static volatile SingularAttribute<MsgLogVO, String> msgId;
    public static volatile SingularAttribute<MsgLogVO, String> msgName;
    public static volatile SingularAttribute<MsgLogVO, String> taskName;
    public static volatile SingularAttribute<MsgLogVO, String> apiId;
    public static volatile SingularAttribute<MsgLogVO, BigDecimal> wait;
    public static volatile SingularAttribute<MsgLogVO, Long> startTime;
    public static volatile SingularAttribute<MsgLogVO, Long> replyTime;
    public static volatile SingularAttribute<MsgLogVO, Integer> status;

}
