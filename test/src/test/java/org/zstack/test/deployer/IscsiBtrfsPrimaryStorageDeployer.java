package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.storage.primary.iscsi.APIAddIscsiFileSystemBackendPrimaryStorageMsg;
import org.zstack.storage.primary.iscsi.IscsiBtrfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiPrimaryStorageConstants;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.IscsiFileSystemPrimaryStorageConfig;

import java.util.List;

/**
 * Created by frank on 4/20/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class IscsiBtrfsPrimaryStorageDeployer implements PrimaryStorageDeployer<IscsiFileSystemPrimaryStorageConfig> {
    @Autowired
    IscsiBtrfsPrimaryStorageSimulatorConfig iconfig;

    @Override
    public void deploy(List<IscsiFileSystemPrimaryStorageConfig> primaryStorages, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api = deployer.getApi();
        for (IscsiFileSystemPrimaryStorageConfig c : primaryStorages) {
            iconfig.availableCapacity = deployer.parseSizeCapacity(c.getAvailableCapacity());
            iconfig.totalCapacity = deployer.parseSizeCapacity(c.getTotalCapacity());

            APIAddIscsiFileSystemBackendPrimaryStorageMsg msg = new APIAddIscsiFileSystemBackendPrimaryStorageMsg();
            msg.setName(c.getName());
            msg.setDescription(c.getDescription());
            msg.setHostname(c.getHostname());
            msg.setFilesystemType(c.getFilesystemType());
            msg.setSshPassword(c.getSshPassword());
            msg.setSshUsername(c.getSshUsername());
            msg.setUrl(c.getUrl());
            msg.setType(IscsiPrimaryStorageConstants.ISCSI_FILE_SYSTEM_BACKEND_PRIMARY_STORAGE_TYPE);
            msg.setZoneUuid(zone.getUuid());
            msg.setSession(api.getAdminSession());
            msg.setChapUsername(c.getChapUsername());
            msg.setChapPassword(c.getChapPassword());
            ApiSender sender = api.getApiSender();
            APIAddPrimaryStorageEvent evt = sender.send(msg, APIAddPrimaryStorageEvent.class);
            PrimaryStorageInventory inv = evt.getInventory();
            deployer.primaryStorages.put(c.getName(), inv);
        }
    }

    @Override
    public Class<IscsiFileSystemPrimaryStorageConfig> getSupportedDeployerClassType() {
        return IscsiFileSystemPrimaryStorageConfig.class;
    }
}
