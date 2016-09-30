package org.zstack.test.deployer;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.List;

public interface HostDeployer<T> extends AbstractDeployer<T> {
    void deploy(List<T> hosts, ClusterInventory cluster, DeployerConfig config, Deployer deployer) throws ApiSenderException;
}
