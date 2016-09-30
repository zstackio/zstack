package org.zstack.test.deployer;

import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.ZoneConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class DefaultZoneDeployer implements ZoneDeployer<ZoneConfig> {
    private CLogger logger = Utils.getLogger(DefaultZoneDeployer.class);

    @Override
    public Class<ZoneConfig> getSupportedDeployerClassType() {
        return ZoneConfig.class;
    }

    @Override
    public void deploy(List<ZoneConfig> zones, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (ZoneConfig zone : zones) {
            ZoneInventory zinv = new ZoneInventory();
            zinv.setName(zone.getName());
            zinv.setDescription(zone.getDescription());
            zinv = deployer.getApi().createZoneByFullConfig(zinv);
            deployer.zones.put(zinv.getName(), zinv);
            deployer.deployCluster(zone.getClusters(), zinv);
            deployer.deployPrimaryStorage(zone.getPrimaryStorages(), zinv);
            deployer.deployL2Network(zone.getL2Networks(), zinv);
            deployer.attachBackupStorage(zone.getBackupStorageRef(), zinv);
        }
    }
}
