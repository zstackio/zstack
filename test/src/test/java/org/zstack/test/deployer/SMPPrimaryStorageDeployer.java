package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.sdk.AddSharedMountPointPrimaryStorageAction;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageConstant;
import org.zstack.storage.primary.smp.SMPPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.SharedMountPointPrimaryStorageConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SMPPrimaryStorageDeployer implements PrimaryStorageDeployer<SharedMountPointPrimaryStorageConfig> {
    @Autowired
    private SMPPrimaryStorageSimulatorConfig simulatorConfig;

    @Override
    public Class<SharedMountPointPrimaryStorageConfig> getSupportedDeployerClassType() {
        return SharedMountPointPrimaryStorageConfig.class;
    }

    @Override
    public void deploy(List<SharedMountPointPrimaryStorageConfig> primaryStorages, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api = deployer.getApi();
        for (SharedMountPointPrimaryStorageConfig nc : primaryStorages) {
            simulatorConfig.totalCapacity = deployer.parseSizeCapacity(nc.getTotalCapacity());
            simulatorConfig.availableCapcacity = deployer.parseSizeCapacity(nc.getAvailableCapacity());
            AddSharedMountPointPrimaryStorageAction action = new AddSharedMountPointPrimaryStorageAction();
            action.name = nc.getName();
            action.url = nc.getUrl();
            action.description = nc.getDescription();
            action.type = NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE;
            action.sessionId = api.getAdminSession().getUuid();
            action.zoneUuid = zone.getUuid();
            AddSharedMountPointPrimaryStorageAction.Result res = action.call().throwExceptionIfError();

            PrimaryStorageInventory inv = JSONObjectUtil.rehashObject(res.value.getInventory(), PrimaryStorageInventory.class);
            deployer.primaryStorages.put(nc.getName(), inv);
        }
    }
}
