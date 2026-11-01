package org.zstack.header.core.external.plugin;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * a plugin in use, or a plugin declaring that it cannot be uninstalled once loaded, is refused;
 * the management nodes restart after the plugin is uninstalled if the plugin needs it
 */
@RestRequest(
        path = "/feature-plugins/{pluginId}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUninstallFeaturePluginEvent.class
)
public class APIUninstallFeaturePluginMsg extends APIMessage {
    @APIParam(maxLength = 48, validRegexValues = "^[a-z][a-z0-9-]{1,47}$")
    private String pluginId;

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public static APIUninstallFeaturePluginMsg __example__() {
        APIUninstallFeaturePluginMsg msg = new APIUninstallFeaturePluginMsg();
        msg.setPluginId("flow-meter");
        return msg;
    }
}
