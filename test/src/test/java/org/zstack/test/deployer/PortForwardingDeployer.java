package org.zstack.test.deployer;

import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.List;

public interface PortForwardingDeployer<T> extends AbstractDeployer<T> {
    void deploy(List<T> portForwardingRules, DeployerConfig config, Deployer deployer1) throws ApiSenderException;
}
