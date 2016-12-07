package org.zstack.test.deployer;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.sdk.CreateClusterAction;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.ClusterConfig;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

public class DefaultClusterDeployer implements ClusterDeployer<ClusterConfig> {

    @Override
    public Class<ClusterConfig> getSupportedDeployerClassType() {
        return ClusterConfig.class;
    }

    @Override
    public void deploy(List<ClusterConfig> clusters, ZoneInventory zone, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (ClusterConfig cc : clusters) {
            CreateClusterAction action = new CreateClusterAction();
            action.name = cc.getName();
            action.description = cc.getDescription();
            action.hypervisorType = cc.getHypervisorType();
            action.zoneUuid = zone.getUuid();
            action.sessionId = deployer.getApi().getAdminSession().getUuid();
            CreateClusterAction.Result res = action.call();

            ClusterInventory cinv = JSONObjectUtil.rehashObject(res.value.getInventory(), ClusterInventory.class);

            deployer.clusters.put(cinv.getName(), cinv);
            deployer.deployHost(cc.getHosts(), cinv);
            deployer.attachPrimaryStorage(cc.getPrimaryStorageRef(), cinv);
            deployer.attachL2Network(cc.getL2NetworkRef(), cinv);
        }
    }

}
