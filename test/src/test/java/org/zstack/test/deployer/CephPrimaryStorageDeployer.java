package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.storage.ceph.primary.APIAddCephPrimaryStorageMsg;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.CephPrimaryStorageConfig;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.SizeUtils;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 7/29/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephPrimaryStorageDeployer implements PrimaryStorageDeployer<CephPrimaryStorageConfig>{
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

            APIAddCephPrimaryStorageMsg msg = new APIAddCephPrimaryStorageMsg();
            msg.setName(c.getName());
            msg.setDescription(c.getDescription());
            msg.setSession(api.getAdminSession());
            msg.setZoneUuid(zone.getUuid());
            msg.setMonUrls(list(c.getMonUrl().split(",")));
            ApiSender sender = api.getApiSender();
            APIAddPrimaryStorageEvent evt = sender.send(msg, APIAddPrimaryStorageEvent.class);
            PrimaryStorageInventory inv = evt.getInventory();
            deployer.primaryStorages.put(inv.getName(), inv);
        }
    }

    @Override
    public Class<CephPrimaryStorageConfig> getSupportedDeployerClassType() {
        return CephPrimaryStorageConfig.class;
    }
}
