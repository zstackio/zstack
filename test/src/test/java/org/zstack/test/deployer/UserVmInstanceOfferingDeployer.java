package org.zstack.test.deployer;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.InstanceOfferingConfig;

import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class UserVmInstanceOfferingDeployer implements InstanceOfferingDeployer<InstanceOfferingConfig> {

    @Override
    public Class<InstanceOfferingConfig> getSupportedDeployerClassType() {
        return InstanceOfferingConfig.class;
    }

    @Override
    public void deploy(List<InstanceOfferingConfig> offerings, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (InstanceOfferingConfig ic : offerings) {
            InstanceOfferingInventory inv = new InstanceOfferingInventory();
            inv.setAllocatorStrategy(ic.getAllocatorStrategy());
            inv.setCpuNum((int) ic.getCpuNum());
            inv.setCpuSpeed((int) ic.getCpuSpeed());
            inv.setDescription(ic.getDescription());
            inv.setMemorySize(deployer.parseSizeCapacity(ic.getMemoryCapacity()));
            inv.setName(ic.getName());

            SessionInventory sessoin = ic.getAccountRef() == null ? null : deployer.loginByAccountRef(ic.getAccountRef(), config);

            inv = deployer.getApi().addInstanceOffering(inv, sessoin);
            deployer.instanceOfferings.put(inv.getName(), inv);
        }
    }

}
