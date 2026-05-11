package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.longjob.APICreateLongJobMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/{serverUuid}/provision",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIProvisionPhysicalServerEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APIProvisionPhysicalServerMsg extends APICreateLongJobMessage {
    @APIParam(resourceType = PhysicalServerVO.class)
    private String serverUuid;

    @APIParam(resourceType = PhysicalServerProvisionNetworkVO.class)
    private String networkUuid;

    @APIParam(resourceType = ImageVO.class)
    private String osImageUuid;

    @APIParam(validValues = {"centos7", "rocky9", "ubuntu22.04"})
    private String osDistribution;

    @APIParam(required = false)
    @NoLogging
    private String kickstartTemplate;

    @APIParam(required = false)
    private String provisionNicMac;

    @APIParam(required = false)
    @NoLogging
    private Map<String, String> customParams;

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public void setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
    }

    public String getOsImageUuid() {
        return osImageUuid;
    }

    public void setOsImageUuid(String osImageUuid) {
        this.osImageUuid = osImageUuid;
    }

    public String getOsDistribution() {
        return osDistribution;
    }

    public void setOsDistribution(String osDistribution) {
        this.osDistribution = osDistribution;
    }

    public String getKickstartTemplate() {
        return kickstartTemplate;
    }

    public void setKickstartTemplate(String kickstartTemplate) {
        this.kickstartTemplate = kickstartTemplate;
    }

    public String getProvisionNicMac() {
        return provisionNicMac;
    }

    public void setProvisionNicMac(String provisionNicMac) {
        this.provisionNicMac = provisionNicMac;
    }

    public Map<String, String> getCustomParams() {
        return customParams;
    }

    public void setCustomParams(Map<String, String> customParams) {
        this.customParams = customParams;
    }

    public static APIProvisionPhysicalServerMsg __example__() {
        APIProvisionPhysicalServerMsg msg = new APIProvisionPhysicalServerMsg();
        msg.setServerUuid(uuid());
        msg.setNetworkUuid(uuid());
        msg.setOsImageUuid(uuid());
        msg.setOsDistribution("rocky9");
        msg.setKickstartTemplate("# kickstart");
        msg.setProvisionNicMac("52:54:00:12:34:56");
        Map<String, String> params = new HashMap<>();
        params.put("username", "root");
        msg.setCustomParams(params);
        return msg;
    }
}
