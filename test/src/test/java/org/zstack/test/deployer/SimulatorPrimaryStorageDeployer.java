package org.zstack.test.deployer;

import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.SimulatorPrimaryStorageConfig;

import java.util.List;

public class SimulatorPrimaryStorageDeployer implements PrimaryStorageDeployer<SimulatorPrimaryStorageConfig> {
    @Override
    public Class<SimulatorPrimaryStorageConfig> getSupportedDeployerClassType() {
        return SimulatorPrimaryStorageConfig.class;
    }

    @Override
    public void deploy(List<SimulatorPrimaryStorageConfig> primaryStorages, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (SimulatorPrimaryStorageConfig pc : primaryStorages) {
            PrimaryStorageInventory pinv = new PrimaryStorageInventory();
            pinv.setDescription(pc.getDescription());
            pinv.setName(pc.getName());
            long tcap = deployer.parseSizeCapacity(pc.getTotalCapacity());
            pinv.setTotalCapacity(tcap);
            long acap = deployer.parseSizeCapacity(pc.getAvailableCapacity());
            pinv.setAvailableCapacity(acap);
            pinv.setUrl(pc.getUrl());
            pinv.setZoneUuid(zone.getUuid());
            pinv = deployer.getApi().addPrimaryStorageByFullConfig(pinv);
            deployer.primaryStorages.put(pinv.getName(), pinv);
        }
    }
}
