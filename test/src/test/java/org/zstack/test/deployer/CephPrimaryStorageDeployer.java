package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.sdk.AddCephPrimaryStorageAction;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.CephPrimaryStorageConfig;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

import static java.util.Arrays.asList;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephPrimaryStorageDeployer implements PrimaryStorageDeployer<CephPrimaryStorageConfig> {
    @Autowired
    private CephPrimaryStorageSimulatorConfig sconfig;

    public void deploy(List<CephPrimaryStorageConfig> primaryStorages, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api = deployer.getApi();
        for (CephPrimaryStorageConfig c : primaryStorages) {
            CephPrimaryStorageSimulatorConfig.CephPrimaryStorageConfig sc = new CephPrimaryStorageSimulatorConfig.CephPrimaryStorageConfig();
            sc.fsid = c.getFsid();
            DebugUtils.Assert(sc.fsid != null, "fsid cannot be null");
            sc.totalCapacity = SizeUtils.sizeStringToBytes(c.getTotalCapacity());
            sc.availCapacity = SizeUtils.sizeStringToBytes(c.getAvailableCapacity());
            sconfig.config.put(c.getName(), sc);

            AddCephPrimaryStorageAction action = new AddCephPrimaryStorageAction();
            action.name = c.getName();
            action.description = c.getDescription();
            action.sessionId = api.getAdminSession().getUuid();
            action.zoneUuid = zone.getUuid();
            action.monUrls = asList(c.getMonUrl().split(","));
            AddCephPrimaryStorageAction.Result res = action.call().throwExceptionIfError();
            deployer.primaryStorages.put(action.name, JSONObjectUtil.rehashObject(res.value.getInventory(), PrimaryStorageInventory.class));
        }
    }

    @Override
    public Class<CephPrimaryStorageConfig> getSupportedDeployerClassType() {
        return CephPrimaryStorageConfig.class;
    }
}
