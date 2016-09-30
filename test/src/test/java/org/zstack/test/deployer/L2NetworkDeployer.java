package org.zstack.test.deployer;

import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.List;

public interface L2NetworkDeployer<T> extends AbstractDeployer<T> {
    void deploy(List<T> l2Networks, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException;
}
