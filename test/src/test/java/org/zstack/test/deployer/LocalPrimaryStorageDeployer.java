package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.sdk.AddLocalPrimaryStorageAction;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.LocalPrimaryStorageConfig;
import org.zstack.utils.gson.JSONObjectUtil;

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

            AddLocalPrimaryStorageAction action = new AddLocalPrimaryStorageAction();
            action.name = nc.getName();
            action.url = nc.getUrl();
            action.description = nc.getDescription();
            action.sessionId = api.getAdminSession().getUuid();
            action.zoneUuid = zone.getUuid();
            AddLocalPrimaryStorageAction.Result res = action.call().throwExceptionIfError();

            PrimaryStorageInventory inv = JSONObjectUtil.rehashObject(res.value.getInventory(), PrimaryStorageInventory.class);
            deployer.primaryStorages.put(nc.getName(), inv);
        }
    }
}
