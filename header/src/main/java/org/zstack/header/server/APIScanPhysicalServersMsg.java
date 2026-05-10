package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

import java.util.List;
import java.util.Map;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/physical-servers/scan",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIScanPhysicalServersEvent.class
)
public class APIScanPhysicalServersMsg extends APIMessage {
    @APIParam(resourceType = ZoneVO.class)
    private String zoneUuid;

    @APIParam(resourceType = ServerPoolVO.class)
    private String poolUuid;

    @APIParam
    private String ipRange;

    @APIParam(required = false)
    private Integer oobPort;

    @APIParam
    @NoLogging
    private List<Map<String, String>> credentials;

    @APIParam(required = false)
    private Integer concurrency;

    @APIParam(required = false)
    private Integer timeoutPerHost;

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public String getIpRange() {
        return ipRange;
    }

    public void setIpRange(String ipRange) {
        this.ipRange = ipRange;
    }

    public Integer getOobPort() {
        return oobPort;
    }

    public void setOobPort(Integer oobPort) {
        this.oobPort = oobPort;
    }

    public List<Map<String, String>> getCredentials() {
        return credentials;
    }

    public void setCredentials(List<Map<String, String>> credentials) {
        this.credentials = credentials;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public Integer getTimeoutPerHost() {
        return timeoutPerHost;
    }

    public void setTimeoutPerHost(Integer timeoutPerHost) {
        this.timeoutPerHost = timeoutPerHost;
    }

    public static APIScanPhysicalServersMsg __example__() {
        APIScanPhysicalServersMsg msg = new APIScanPhysicalServersMsg();
        msg.setZoneUuid(uuid());
        msg.setPoolUuid(uuid());
        msg.setIpRange("192.168.1.100-192.168.1.200");
        msg.setOobPort(623);
        java.util.Map<String, String> cred = new java.util.HashMap<>();
        cred.put("username", "admin");
        cred.put("password", "password");
        msg.setCredentials(java.util.Arrays.asList(cred));
        msg.setConcurrency(20);
        msg.setTimeoutPerHost(3);
        return msg;
    }
}
