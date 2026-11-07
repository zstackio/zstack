package org.zstack.header.core.external.plugin;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * every management node loads the plugin without restarting; the install is undone if any of them cannot
 */
@RestRequest(
        path = "/feature-plugins",
        method = HttpMethod.POST,
        responseClass = APIInstallFeaturePluginEvent.class,
        parameterName = "params"
)
public class APIInstallFeaturePluginMsg extends APIMessage {
    @APIParam(maxLength = 1024)
    private String packagePath;

    public String getPackagePath() {
        return packagePath;
    }

    public void setPackagePath(String packagePath) {
        this.packagePath = packagePath;
    }

    public static APIInstallFeaturePluginMsg __example__() {
        APIInstallFeaturePluginMsg msg = new APIInstallFeaturePluginMsg();
        msg.setPackagePath(FeaturePluginPackageInventory.__example__().getPath());
        return msg;
    }
}
