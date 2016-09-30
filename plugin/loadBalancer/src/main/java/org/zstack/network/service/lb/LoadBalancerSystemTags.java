package org.zstack.network.service.lb;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by frank on 8/9/2015.
 */
@TagDefinition
public class LoadBalancerSystemTags {
    public static PatternedSystemTag SEPARATE_VR = new PatternedSystemTag("separateVirtualRouterVm", LoadBalancerVO.class);

    public static final String HEALTHY_THRESHOLD_TOKEN = "healthyThreshold";
    public static PatternedSystemTag HEALTHY_THRESHOLD = new PatternedSystemTag(String.format("healthyThreshold::{%s}", HEALTHY_THRESHOLD_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String HEALTH_INTERVAL_TOKEN = "healthCheckInterval";
    public static PatternedSystemTag HEALTH_INTERVAL = new PatternedSystemTag(String.format("healthCheckInterval::{%s}", HEALTH_INTERVAL_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String HEALTH_TARGET_TOKEN = "healthCheckTarget";
    public static PatternedSystemTag HEALTH_TARGET = new PatternedSystemTag(String.format("healthCheckTarget::{%s}", HEALTH_TARGET_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String HEALTH_TIMEOUT_TOKEN = "healthCheckTimeout";
    public static PatternedSystemTag HEALTH_TIMEOUT = new PatternedSystemTag(String.format("healthCheckTimeout::{%s}", HEALTH_TIMEOUT_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String UNHEALTHY_THRESHOLD_TOKEN = "unhealthyThreshold";
    public static PatternedSystemTag UNHEALTHY_THRESHOLD = new PatternedSystemTag(String.format("unhealthyThreshold::{%s}", UNHEALTHY_THRESHOLD_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String CONNECTION_IDLE_TIMEOUT_TOKEN = "connectionIdleTimeout";
    public static PatternedSystemTag CONNECTION_IDLE_TIMEOUT = new PatternedSystemTag(String.format("connectionIdleTimeout::{%s}", CONNECTION_IDLE_TIMEOUT_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String MAX_CONNECTION_TOKEN = "maxConnection";
    public static PatternedSystemTag MAX_CONNECTION = new PatternedSystemTag(String.format("maxConnection::{%s}", MAX_CONNECTION_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String BALANCER_ALGORITHM_TOKEN = "balancerAlgorithm";
    public static PatternedSystemTag BALANCER_ALGORITHM = new PatternedSystemTag(String.format("balancerAlgorithm::{%s}", BALANCER_ALGORITHM_TOKEN), LoadBalancerListenerVO.class);
}
