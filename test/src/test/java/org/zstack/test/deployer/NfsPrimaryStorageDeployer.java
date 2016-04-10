package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.nfs.APIAddNfsPrimaryStorageMsg;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.NfsPrimaryStorageConfig;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsPrimaryStorageDeployer implements PrimaryStorageDeployer<NfsPrimaryStorageConfig> {
    @Autowired
    private NfsPrimaryStorageSimulatorConfig nfsSimulatorConfig;
    
    @Override
    public Class<NfsPrimaryStorageConfig> getSupportedDeployerClassType() {
        return NfsPrimaryStorageConfig.class;
    }

    @Override
    public void deploy(List<NfsPrimaryStorageConfig> primaryStorages, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api = deployer.getApi();
        for (NfsPrimaryStorageConfig nc : primaryStorages) {
            nfsSimulatorConfig.totalCapacity = deployer.parseSizeCapacity(nc.getTotalCapacity());
            nfsSimulatorConfig.availableCapacity = deployer.parseSizeCapacity(nc.getAvailableCapacity());
            APIAddNfsPrimaryStorageMsg msg = new APIAddNfsPrimaryStorageMsg();
            msg.setName(nc.getName());
            msg.setUrl(nc.getUrl());
            msg.setDescription(nc.getDescription());
            msg.setType(NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);
            msg.setSession(api.getAdminSession());
            msg.setZoneUuid(zone.getUuid());
            if (nc.getOptions() != null) {
                msg.addSystemTag(nc.getOptions());
            }
            ApiSender sender = api.getApiSender();
            APIAddPrimaryStorageEvent evt = sender.send(msg, APIAddPrimaryStorageEvent.class);
            PrimaryStorageInventory inv = evt.getInventory(); 
            deployer.primaryStorages.put(nc.getName(), inv);
        }
    }
}
