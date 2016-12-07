package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.sdk.AddImageStoreBackupStorageAction;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.ImageStoreBackupStorageConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ImageStoreBackupStorageDeployer implements BackupStorageDeployer<ImageStoreBackupStorageConfig> {
    @Autowired
    private ImageStoreBackupStorageSimulatorConfig simulatorConfig;

    @Override
    public Class<ImageStoreBackupStorageConfig> getSupportedDeployerClassType() {
        return ImageStoreBackupStorageConfig.class;
    }

    @Override
    public void deploy(List<ImageStoreBackupStorageConfig> backupStorages, DeployerConfig config, Deployer deployer)
            throws ApiSenderException {
        Api api = deployer.getApi();
        for (ImageStoreBackupStorageConfig bs : backupStorages) {
            simulatorConfig.totalCapacity = deployer.parseSizeCapacity(bs.getTotalCapacity());
            simulatorConfig.availableCapacity = deployer.parseSizeCapacity(bs.getAvailableCapacity());

            AddImageStoreBackupStorageAction action = new AddImageStoreBackupStorageAction();
            action.sessionId = api.getAdminSession().getUuid();
            action.name = bs.getName();
            action.url = bs.getUrl();
            action.type = ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE;
            action.hostname = bs.getHostname();
            action.username = bs.getUsername();
            action.password = bs.getPassword();
            AddImageStoreBackupStorageAction.Result res = action.call().throwExceptionIfError();

            BackupStorageInventory inv = JSONObjectUtil.rehashObject(res.value.getInventory(), BackupStorageInventory.class);
            deployer.backupStorages.put(bs.getName(), inv);
        }
    }

}
