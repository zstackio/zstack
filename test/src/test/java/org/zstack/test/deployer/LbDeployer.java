package org.zstack.test.deployer;

import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.List;

/**
 * Created by frank on 8/10/2015.
 */
public interface LbDeployer<T> extends AbstractDeployer {
    void deploy(List<T> lbs, DeployerConfig config, Deployer deployer) throws ApiSenderException;
}
