package org.zstack.test.deployer;

import org.zstack.header.configuration.APICreateInstanceOfferingEvent;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.mevoco.APICreateConvergedOfferingMsg;
import org.zstack.mevoco.MevocoConstants;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.ConvergedOfferingConfig;
import org.zstack.test.deployer.schema.DeployerConfig;

import java.util.List;

/**
 * Created by frank on 9/18/2015.
 */
public class ConvergedOfferingDeployer implements InstanceOfferingDeployer<ConvergedOfferingConfig> {
    @Override
    public void deploy(List<ConvergedOfferingConfig> offerings, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (ConvergedOfferingConfig ic : offerings) {
            APICreateConvergedOfferingMsg msg = new APICreateConvergedOfferingMsg();

            SessionInventory session = ic.getAccountRef() == null ? deployer.getApi().getAdminSession() : deployer.loginByAccountRef(ic.getAccountRef(), config);
            msg.setSession(session);
            msg.setAllocatorStrategy(ic.getAllocatorStrategy());
            msg.setCpuNum((int) ic.getCpuNum());
            msg.setCpuSpeed((int) ic.getCpuSpeed());
            msg.setDescription(ic.getDescription());
            msg.setMemorySize(deployer.parseSizeCapacity(ic.getMemoryCapacity()));
            msg.setName(ic.getName());
            msg.setType(MevocoConstants.CONVERGED_OFFERING_TYPE);

            msg.setNetworkBandwidth(ic.getNetworkBandwidth());
            msg.setVolumeBandwidth(ic.getVolumeBandwidth());

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
