package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.sdk.AddSftpBackupStorageAction;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.SftpBackupStorageConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.SftpBackupStorageConfig;
import org.zstack.utils.gson.JSONObjectUtil;

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

            AddSftpBackupStorageAction action = new AddSftpBackupStorageAction();
            action.sessionId = api.getAdminSession().getUuid();
            action.name = bs.getName();
            action.url = bs.getUrl();
            action.type = SftpBackupStorageConstant.SFTP_BACKUP_STORAGE_TYPE;
            action.hostname = bs.getHostname();
            action.username = bs.getUsername();
            action.password = bs.getPassword();
            AddSftpBackupStorageAction.Result res = action.call().throwExceptionIfError();

            BackupStorageInventory inv = JSONObjectUtil.rehashObject(res.value.getInventory(), BackupStorageInventory.class);
            deployer.backupStorages.put(bs.getName(), inv);
        }
    }

}
