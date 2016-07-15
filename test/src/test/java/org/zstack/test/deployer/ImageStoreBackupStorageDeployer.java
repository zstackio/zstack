package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.storage.backup.imagestore.APIAddImageStoreBackupStorageEvent;
import org.zstack.storage.backup.imagestore.APIAddImageStoreBackupStorageMsg;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.ImageStoreBackupStorageConfig;

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
            APIAddImageStoreBackupStorageMsg msg = new APIAddImageStoreBackupStorageMsg();
            msg.setSession(api.getAdminSession());
            msg.setName(bs.getName());
            msg.setUrl(bs.getUrl());
            msg.setType(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE);
            msg.setHostname(bs.getHostname());
            msg.setUsername(bs.getUsername());
            msg.setPassword(bs.getPassword());
            ApiSender sender = api.getApiSender();
            APIAddBackupStorageEvent evt = sender.send(msg, APIAddImageStoreBackupStorageEvent.class);
            BackupStorageInventory inv = evt.getInventory();
            deployer.backupStorages.put(bs.getName(), inv);
        }
    }

}
