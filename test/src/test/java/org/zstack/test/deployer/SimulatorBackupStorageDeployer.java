package org.zstack.test.deployer;

import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.SimulatorBackupStorageConfig;

import java.util.List;

public class SimulatorBackupStorageDeployer implements BackupStorageDeployer<SimulatorBackupStorageConfig> {
    @Override
    public Class<SimulatorBackupStorageConfig> getSupportedDeployerClassType() {
        return SimulatorBackupStorageConfig.class;
    }

    @Override
    public void deploy(List<SimulatorBackupStorageConfig> backupStorages, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (SimulatorBackupStorageConfig bc : backupStorages) {
            BackupStorageInventory binv = new BackupStorageInventory();
            binv.setDescription(bc.getDescription());
            binv.setName(bc.getName());
            long tcap = deployer.parseSizeCapacity(bc.getTotalCapacity());
            binv.setTotalCapacity(tcap);
            binv.setUrl(bc.getUrl());
            long acap = deployer.parseSizeCapacity(bc.getAvailableCapacity());
            binv.setAvailableCapacity(acap);
            binv = deployer.getApi().addBackupStorageByFullConfig(binv);
            deployer.backupStorages.put(binv.getName(), binv);
        }
    }
}
