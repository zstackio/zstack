package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.sdk.AddCephBackupStorageAction;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.CephBackupStorageConfig;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

import static java.util.Arrays.asList;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephBackupStorageDeployer implements BackupStorageDeployer<CephBackupStorageConfig> {
    @Autowired
    private CephBackupStorageSimulatorConfig sconfig;

    @Override
    public void deploy(List<CephBackupStorageConfig> backupStorages, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api = deployer.getApi();
        for (CephBackupStorageConfig c : backupStorages) {
            CephBackupStorageSimulatorConfig.CephBackupStorageConfig sc = new CephBackupStorageSimulatorConfig.CephBackupStorageConfig();
            sc.fsid = c.getFsid();
            sc.availCapacity = SizeUtils.sizeStringToBytes(c.getAvailableCapacity());
            sc.totalCapacity = SizeUtils.sizeStringToBytes(c.getTotalCapacity());
            sconfig.config.put(c.getName(), sc);

            AddCephBackupStorageAction action = new AddCephBackupStorageAction();
            action.monUrls = asList(c.getMonUrl().split(","));
            action.sessionId = api.getAdminSession().getUuid();
            action.name = c.getName();
            AddCephBackupStorageAction.Result res = action.call().throwExceptionIfError();
            deployer.backupStorages.put(action.name, JSONObjectUtil.rehashObject(res.value.getInventory(), BackupStorageInventory.class));
        }
    }

    @Override
    public Class<CephBackupStorageConfig> getSupportedDeployerClassType() {
        return CephBackupStorageConfig.class;
    }
}
