package org.zstack.plugin.netprobe;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/net-probe/tasks",
        method = HttpMethod.POST,
        responseClass = APICreateNetProbeTaskEvent.class,
        parameterName = "params"
)
public class APICreateNetProbeTaskMsg extends APIMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    @APIParam(validValues = {"ICMP", "TCP", "HTTP"})
    private String type;

    @APIParam(required = false, maxLength = 253)
    private String target;

    @APIParam(required = false, numberRange = {1, 65535})
    private Integer port;

    @APIParam(required = false, maxLength = 2048)
    private String url;

    @APIParam(required = false, numberRange = {10, 3600})
    private Integer intervalSeconds;

    @APIParam(required = false, numberRange = {1, 30})
    private Integer timeoutSeconds;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
