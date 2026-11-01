package org.zstack.header.core.external.plugin;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/feature-plugins/packages",
        method = HttpMethod.POST,
        responseClass = APIAddFeaturePluginPackageEvent.class,
        parameterName = "params"
)
public class APIAddFeaturePluginPackageMsg extends APIMessage {
    @APIParam(maxLength = 255, validRegexValues = "^[A-Za-z0-9._-]+\\.zpkg$")
    private String name;

    /**
     * base64 encoded package content
     */
    @APIParam
    @NoLogging
    private String content;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public static APIAddFeaturePluginPackageMsg __example__() {
        APIAddFeaturePluginPackageMsg msg = new APIAddFeaturePluginPackageMsg();
        msg.setName("flowMeter-5.5.48-1.zpkg");
        msg.setContent("H4sIAAAAAAAAA+3OMQ6CQBCF4a3XKw==");
        return msg;
    }
}
