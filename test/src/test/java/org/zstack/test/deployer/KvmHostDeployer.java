package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.KvmHostConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;

import java.util.List;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class KvmHostDeployer implements HostDeployer<KvmHostConfig> {
    @Autowired
    private KVMSimulatorConfig simulatorConfig;
    
    @Override
    public Class<KvmHostConfig> getSupportedDeployerClassType() {
        return KvmHostConfig.class;
    }

    @Override
    public void deploy(List<KvmHostConfig> hosts, ClusterInventory cluster, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        Api api = deployer.getApi();
        for (KvmHostConfig kc : hosts) {
            simulatorConfig.cpuNum = kc.getCpuNum();
            simulatorConfig.cpuSpeed = kc.getCpuSpeed();
            simulatorConfig.totalMemory = deployer.parseSizeCapacity(kc.getMemoryCapacity());
            APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
            msg.setName(kc.getName());
            msg.setClusterUuid(cluster.getUuid());
            msg.setManagementIp(kc.getManagementIp());
            msg.setUsername(kc.getUsername());
            msg.setPassword(kc.getPassword());
            msg.setSession(api.getAdminSession());
            ApiSender sender = api.getApiSender();
            APIAddHostEvent evt = sender.send(msg, APIAddHostEvent.class);
            deployer.hosts.put(kc.getName(), evt.getInventory());
        }
    }
}
