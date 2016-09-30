package org.zstack.test.deployer;

import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.List;

public interface ClusterDeployer<T> extends AbstractDeployer<T> {
    void deploy(List<T> clusters, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException;
}
