package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin.ruan on 02/25/2019.
 */
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/listeners/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeLoadBalancerListenerEvent.class,
        isAction = true
)
public class APIChangeLoadBalancerListenerMsg extends APIMessage implements LoadBalancerListenerMsg , LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @APIParam(numberRange = {LoadBalancerConstants.CONNECTION_IDLE_TIMEOUT_MIN, LoadBalancerConstants.CONNECTION_IDLE_TIMEOUT_MAX}, required = false)
    private Integer connectionIdleTimeout;

    @APIParam(numberRange = {LoadBalancerConstants.MAXIMUM_CONNECTION_MIN, LoadBalancerConstants.MAXIMUM_CONNECTION_MAX}, required = false)
    private Integer maxConnection;

    @APIParam(validValues = {"roundrobin", "leastconn", "source"}, required = false)
    private String balancerAlgorithm;

    @APIParam(required = false)
    private String healthCheckTarget;

    @APIParam(numberRange = {LoadBalancerConstants.HEALTH_CHECK_THRESHOLD_MIN, LoadBalancerConstants.HEALTH_CHECK_THRESHOLD_MAX}, required = false)
    private Integer healthyThreshold;

    @APIParam(numberRange = {LoadBalancerConstants.UNHEALTH_CHECK_THRESHOLD_MIN, LoadBalancerConstants.UNHEALTH_CHECK_THRESHOLD_MAX}, required = false)
    private Integer unhealthyThreshold;

    @APIParam(numberRange = {LoadBalancerConstants.HEALTH_CHECK_INTERVAL_MIN, LoadBalancerConstants.HEALTH_CHECK_INTERVAL_MAX}, required = false)
    private Integer healthCheckInterval;

    @APINoSee
    private String loadBalancerUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getConnectionIdleTimeout() {
        return connectionIdleTimeout;
    }

    public void setConnectionIdleTimeout(Integer connectionIdleTimeout) {
        this.connectionIdleTimeout = connectionIdleTimeout;
    }

    public Integer getMaxConnection() {
        return maxConnection;
    }

    public void setMaxConnection(Integer maxConnection) {
        this.maxConnection = maxConnection;
    }

    public String getBalancerAlgorithm() {
        return balancerAlgorithm;
    }

    public void setBalancerAlgorithm(String balancerAlgorithm) {
        this.balancerAlgorithm = balancerAlgorithm;
    }

    public String getHealthCheckTarget() {
        return healthCheckTarget;
    }

    public void setHealthCheckTarget(String healthCheckTarget) {
        this.healthCheckTarget = healthCheckTarget;
    }

    public Integer getHealthyThreshold() {
        return healthyThreshold;
    }

    public void setHealthyThreshold(Integer healthyThreshold) {
        this.healthyThreshold = healthyThreshold;
    }

    public Integer getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    public void setUnhealthyThreshold(Integer unhealthyThreshold) {
        this.unhealthyThreshold = unhealthyThreshold;
    }

    public Integer getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(Integer healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    @Override
    public String getLoadBalancerListenerUuid() {
        return uuid;
    }

    public static APIChangeLoadBalancerListenerMsg __example__() {
        APIChangeLoadBalancerListenerMsg msg = new APIChangeLoadBalancerListenerMsg();

        msg.setUuid(uuid());
        msg.setBalancerAlgorithm("roundrobin");
        msg.setConnectionIdleTimeout(300);
        msg.setHealthCheckInterval(5);
        msg.setHealthCheckTarget("default");
        msg.setHealthyThreshold(2);
        msg.setMaxConnection(5000);
        msg.setUnhealthyThreshold(3);

        return msg;
    }
}
