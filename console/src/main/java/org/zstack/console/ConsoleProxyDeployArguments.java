package org.zstack.console;

import com.google.gson.annotations.SerializedName;
import org.zstack.core.ansible.SyncTimeRequestedDeployArguments;

public class ConsoleProxyDeployArguments extends SyncTimeRequestedDeployArguments {
    @SerializedName("pkg_consoleproxy")
    private final String packageName = ConsoleGlobalProperty.AGENT_PACKAGE_NAME;
    @SerializedName("http_console_proxy_port")
    private Integer httpConsoleProxyPort;

    @Override
    public String getPackageName() {
        return packageName;
    }

    public Integer getHttpConsoleProxyPort() {
        return httpConsoleProxyPort;
    }

    public void setHttpConsoleProxyPort(Integer httpConsoleProxyPort) {
        this.httpConsoleProxyPort = httpConsoleProxyPort;
    }
}
