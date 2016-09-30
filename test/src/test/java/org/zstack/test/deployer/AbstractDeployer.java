package org.zstack.test.deployer;

public interface AbstractDeployer<T> {
    Class<T> getSupportedDeployerClassType();
}
