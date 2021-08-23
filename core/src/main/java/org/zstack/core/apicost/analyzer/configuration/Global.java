package org.zstack.core.apicost.analyzer.configuration;

import org.zstack.core.apicost.analyzer.zstack.Predictor;

/**
 * Created by huaxin on 2021/7/9.
 */
public class Global {

    // 系统更新步骤图的时间为1小时一次
    public static final long STEP_GRAPH_UPDATE_PERIOD = 3600;

    //public static Predictor PREDICTOR = new Predictor();
    public static Predictor PREDICTOR;
}
