package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.local.APIAddLocalPrimaryStorageMsg;
import org.zstack.storage.primary.local.LocalStorageConstants;
import org.zstack.storage.primary.nfs.APIAddNfsPrimaryStorageMsg;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.LocalPrimaryStorageConfig;
import org.zstack.test.deployer.schema.NfsPrimaryStorageConfig;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LocalPrimaryStorageDeployer implements PrimaryStorageDeployer<LocalPrimaryStorageConfig> {
    @Override
    public Class<LocalPrimaryStorageConfig> getSupportedDeployerClassType() {
        return LocalPrimaryStorageConfig.class;
    }

    @Override
    public void deploy(List<LocalPrimaryStorageConfig> primaryStorages, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api = deployer.getApi();
        for (LocalPrimaryStorageConfig nc : primaryStorages) {
            APIAddLocalPrimaryStorageMsg msg = new APIAddLocalPrimaryStorageMsg();
            msg.setName(nc.getName());
            msg.setUrl(nc.getUrl());
            msg.setDescription(nc.getDescription());
            msg.setSession(api.getAdminSession());
            msg.setZoneUuid(zone.getUuid());
            ApiSender sender = api.getApiSender();
            APIAddPrimaryStorageEvent evt = sender.send(msg, APIAddPrimaryStorageEvent.class);
            PrimaryStorageInventory inv = evt.getInventory(); 
            deployer.primaryStorages.put(nc.getName(), inv);
        }
    }
}
