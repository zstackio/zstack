package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.storage.ceph.backup.APIAddCephBackupStorageMsg;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.CephBackupStorageConfig;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.SizeUtils;
import org.zstack.utils.data.SizeUnit;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/29/2015.
 */
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

            APIAddCephBackupStorageMsg msg = new APIAddCephBackupStorageMsg();
            DebugUtils.Assert(c.getMonUrl() != null, "monUrl cannot be null");
            msg.setMonUrls(list(c.getMonUrl().split(",")));
            msg.setSession(api.getAdminSession());
            msg.setName(c.getName());
            ApiSender sender = api.getApiSender();
            APIAddBackupStorageEvent evt = sender.send(msg, APIAddBackupStorageEvent.class);
            BackupStorageInventory inv = evt.getInventory();
            deployer.backupStorages.put(inv.getName(), inv);
        }
    }

    @Override
    public Class<CephBackupStorageConfig> getSupportedDeployerClassType() {
        return CephBackupStorageConfig.class;
    }
}
