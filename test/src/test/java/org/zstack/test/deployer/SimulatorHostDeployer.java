package org.zstack.test.deployer;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.SimulatorHostConfig;

import java.util.List;

public class SimulatorHostDeployer implements HostDeployer<SimulatorHostConfig> {

    static {
        Deployer.registerDeployer(new SimulatorHostDeployer());
    }

    @Override
    public Class<SimulatorHostConfig> getSupportedDeployerClassType() {
        return SimulatorHostConfig.class;
    }

    @Override
    public void deploy(List<SimulatorHostConfig> hosts, ClusterInventory cluster, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (SimulatorHostConfig hc : hosts) {
            HostInventory hinv = new HostInventory();
            hinv.setClusterUuid(cluster.getUuid());
            hinv.setDescription(hc.getDescription());
            hinv.setManagementIp(hc.getManagementIp());
            hinv.setName(hc.getName());
            hinv.setManagementIp(hc.getManagementIp());
            long memCap = deployer.parseSizeCapacity(hc.getMemoryCapacity());
            long cpuCap = hc.getCpuNum() * hc.getCpuSpeed();
            hinv.setAvailableMemoryCapacity(memCap);
            hinv.setAvailableCpuCapacity(cpuCap);
            hinv = deployer.getApi().addHostByFullConfig(hinv);
            deployer.hosts.put(hinv.getName(), hinv);
        }
    }
}
