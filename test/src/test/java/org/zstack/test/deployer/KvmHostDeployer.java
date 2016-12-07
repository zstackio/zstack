package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.sdk.AddKVMHostAction;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.KvmHostConfig;
import org.zstack.utils.gson.JSONObjectUtil;

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

            AddKVMHostAction action = new AddKVMHostAction();
            action.name = kc.getName();
            action.clusterUuid = cluster.getUuid();
            action.managementIp = kc.getManagementIp();
            action.username = kc.getUsername();
            action.password = kc.getPassword();
            action.sessionId = api.getAdminSession().getUuid();
            AddKVMHostAction.Result res = action.call().throwExceptionIfError();

            HostInventory inv = JSONObjectUtil.rehashObject(res.value.getInventory(), HostInventory.class);
            deployer.hosts.put(kc.getName(), inv);
        }
    }
}
