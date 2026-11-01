package org.zstack.header.core.external.plugin;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * the management node restarts after the plugin is uninstalled
 */
@RestRequest(
        path = "/feature-plugins/{pluginId}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUninstallFeaturePluginEvent.class
)
public class APIUninstallFeaturePluginMsg extends APIMessage {
    @APIParam(maxLength = 255, validRegexValues = "^[A-Za-z0-9_-]+$")
    private String pluginId;

    /**
     * uninstall even if the plugin still has resources
     */
    @APIParam(required = false)
    private boolean force;

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public static APIUninstallFeaturePluginMsg __example__() {
        APIUninstallFeaturePluginMsg msg = new APIUninstallFeaturePluginMsg();
        msg.setPluginId("flowMeter");
        return msg;
    }
}
