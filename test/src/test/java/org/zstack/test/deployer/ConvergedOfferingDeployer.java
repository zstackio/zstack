package org.zstack.test.deployer;

import org.zstack.header.configuration.APICreateInstanceOfferingEvent;
import org.zstack.header.configuration.APICreateInstanceOfferingMsg;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.ConvergedOfferingConfig;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 9/18/2015.
 */
public class ConvergedOfferingDeployer implements InstanceOfferingDeployer<ConvergedOfferingConfig> {
    @Override
    public void deploy(List<ConvergedOfferingConfig> offerings, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (ConvergedOfferingConfig ic : offerings) {
            APICreateInstanceOfferingMsg msg = new APICreateInstanceOfferingMsg();

            SessionInventory session = ic.getAccountRef() == null ? deployer.getApi().getAdminSession() : deployer.loginByAccountRef(ic.getAccountRef(), config);
            msg.setSession(session);
            msg.setAllocatorStrategy(ic.getAllocatorStrategy());
            msg.setCpuNum((int) ic.getCpuNum());
            msg.setCpuSpeed((int) ic.getCpuSpeed());
            msg.setDescription(ic.getDescription());
            msg.setMemorySize(deployer.parseSizeCapacity(ic.getMemoryCapacity()));
            msg.setName(ic.getName());
            msg.setType(ConfigurationConstant.USER_VM_INSTANCE_OFFERING_TYPE);

            List<String> systemTags = new ArrayList<String>();
            if (ic.getNetworkOutboundBandwidth() != null) {
                systemTags.add(String.format("networkOutboundBandwidth::%s", ic.getNetworkOutboundBandwidth()));
            }
            if (ic.getNetworkInboundBandwidth() != null) {
                systemTags.add(String.format("networkInboundBandwidth::%s", ic.getNetworkInboundBandwidth()));
            }
            if (ic.getVolumeTotalBandwidth() != null) {
                systemTags.add(String.format("volumeTotalBandwidth::%s", ic.getVolumeTotalBandwidth()));
            }
            if (ic.getVolumeTotalIops() != null) {
                systemTags.add(String.format("volumeTotalIops::%s", ic.getVolumeTotalIops()));
            }

            msg.setSystemTags(systemTags);

            ApiSender sender = deployer.getApi().getApiSender();
            APICreateInstanceOfferingEvent evt = sender.send(msg, APICreateInstanceOfferingEvent.class);
            InstanceOfferingInventory inv = evt.getInventory();
            deployer.instanceOfferings.put(inv.getName(), inv);
        }
    }

    @Override
    public Class<ConvergedOfferingConfig> getSupportedDeployerClassType() {
        return ConvergedOfferingConfig.class;
    }
}
