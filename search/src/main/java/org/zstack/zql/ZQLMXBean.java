package org.zstack.zql;

import java.util.Map;

public interface ZQLMXBean {
    Map<String, ZQLStatistic> getZQLStatistic();
}
