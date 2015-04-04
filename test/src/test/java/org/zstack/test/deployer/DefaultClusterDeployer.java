package org.zstack.test.deployer;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.ClusterConfig;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.List;

public class DefaultClusterDeployer implements ClusterDeployer<ClusterConfig> {
    
    @Override
    public Class<ClusterConfig> getSupportedDeployerClassType() {
        return ClusterConfig.class;
    }

    @Override
    public void deploy(List<ClusterConfig> clusters, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (ClusterConfig cc : clusters) {
            ClusterInventory cinv = new ClusterInventory();
            cinv.setName(cc.getName());
            cinv.setDescription(cc.getDescription());
            cinv.setHypervisorType(cc.getHypervisorType());
            cinv.setZoneUuid(zone.getUuid());
            cinv = deployer.getApi().createClusterByFullConfig(cinv);
            deployer.clusters.put(cinv.getName(), cinv);
            deployer.deployHost(cc.getHosts(), cinv);
            deployer.attachPrimaryStorage(cc.getPrimaryStorageRef(), cinv);
            deployer.attachL2Network(cc.getL2NetworkRef(), cinv);
        }
    }

}
