package org.zstack.resourceconfig;

import java.util.Map;
import java.util.Set;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/1/21 15:18
 */

public abstract class ClusterResourceConfig {

    public abstract Map<String,String> getResourceConfigDefaultValueMap();

    public abstract String getArchitecture();

    public abstract Set<String> getAutoSetIfNotConfiguredClusterResourceConfigSet();
}
