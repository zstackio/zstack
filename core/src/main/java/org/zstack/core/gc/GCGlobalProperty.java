package org.zstack.core.gc;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/5/17.
 */
@GlobalPropertyDefinition
public class GCGlobalProperty {
    @GlobalProperty(name="GC.scanJob.interval", defaultValue = "5m")
    public static String SCAN_JOB_INTERVAL;
}
