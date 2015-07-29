package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.CephBackupStorageConfig;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.List;

/**
 * Created by frank on 7/29/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephBackupStorageDeployer implements BackupStorageDeployer<CephBackupStorageConfig> {
    @Override
    public void deploy(List<CephBackupStorageConfig> backupStorages, DeployerConfig config, Deployer deployer) throws ApiSenderException {

    }

    @Override
    public Class<CephBackupStorageConfig> getSupportedDeployerClassType() {
        return null;
    }
}
