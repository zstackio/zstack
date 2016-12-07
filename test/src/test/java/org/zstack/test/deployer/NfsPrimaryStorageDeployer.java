package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.sdk.AddNfsPrimaryStorageAction;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.NfsPrimaryStorageConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

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

            AddNfsPrimaryStorageAction action = new AddNfsPrimaryStorageAction();
            action.name = nc.getName();
            action.url = nc.getUrl();
            action.description = nc.getDescription();
            action.type = NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE;
            action.sessionId = api.getAdminSession().getUuid();
            action.zoneUuid = zone.getUuid();
            if (nc.getOptions() != null) {
                action.systemTags = asList(nc.getOptions());
            }
            AddNfsPrimaryStorageAction.Result res = action.call();

            PrimaryStorageInventory inv = JSONObjectUtil.rehashObject(res.value.getInventory(), PrimaryStorageInventory.class);
            deployer.primaryStorages.put(nc.getName(), inv);
        }
    }
}
