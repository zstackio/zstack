package org.zstack.header.cluster;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by GuoYi on 3/12/18
 */
@RestRequest(
        path = "/clusters/{uuid}/actions",
        responseClass = APIUpdateClusterOSEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APIUpdateClusterOSMsg extends APICreateMessage implements ClusterMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String uuid;
    @APIParam(required = false, nonempty = true)
    private List<String> excludePackages;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getExcludePackages() {
        return excludePackages;
    }

    public void setExcludePackages(List<String> excludePackages) {
        this.excludePackages = excludePackages;
    }

    @Override
    public String getClusterUuid() {
        return uuid;
    }

    public static APIUpdateClusterOSMsg __example__() {
        APIUpdateClusterOSMsg msg = new APIUpdateClusterOSMsg();
        msg.setUuid(uuid());
        msg.setExcludePackages(Arrays.asList("kernel", "systemd*"));
        return msg;
    }
}
