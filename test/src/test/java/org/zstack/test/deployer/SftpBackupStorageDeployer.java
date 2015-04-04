package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.storage.backup.sftp.APIAddSftpBackupStorageEvent;
import org.zstack.storage.backup.sftp.APIAddSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.SftpBackupStorageConfig;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;

import java.util.List;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SftpBackupStorageDeployer implements BackupStorageDeployer<SftpBackupStorageConfig> {
    @Autowired
    private SftpBackupStorageSimulatorConfig simulatorConfig;
    
    @Override
    public Class<SftpBackupStorageConfig> getSupportedDeployerClassType() {
        return SftpBackupStorageConfig.class;
    }

    @Override
    public void deploy(List<SftpBackupStorageConfig> backupStorages, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api = deployer.getApi();
        for (SftpBackupStorageConfig bs : backupStorages) {
            simulatorConfig.totalCapacity = deployer.parseSizeCapacity(bs.getTotalCapacity());
            simulatorConfig.availableCapacity = deployer.parseSizeCapacity(bs.getAvailableCapacity());
            APIAddSftpBackupStorageMsg msg = new APIAddSftpBackupStorageMsg();
            msg.setSession(api.getAdminSession());
            msg.setName(bs.getName());
            msg.setUrl(bs.getUrl());
            msg.setType(SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE);
            msg.setHostname(bs.getHostname());
            msg.setUsername(bs.getUsername());
            msg.setPassword(bs.getPassword());
            ApiSender sender = api.getApiSender();
            APIAddBackupStorageEvent evt = sender.send(msg, APIAddSftpBackupStorageEvent.class);
            BackupStorageInventory inv = evt.getInventory();
            deployer.backupStorages.put(bs.getName(), inv);
        }
    }

}
