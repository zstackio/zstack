package org.zstack.sdnController.znsproxy;

import java.util.List;

public class ZnsProxyPrepareServiceCmd {
    public static final String COMMAND_PATH = "/zns/notify/prepare-service";

    public String computeManagerUuid;
    public List<String> hostUuids;
    public String proxyVersion;
    public String packageName;
}
