package org.zstack.test.deployer;

import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.List;

public interface L3NetworkDeployer<T> extends AbstractDeployer<T> {
    void deploy(List<T> l3Networks, L2NetworkInventory l2Network, DeployerConfig config, Deployer deployer) throws ApiSenderException;
}
