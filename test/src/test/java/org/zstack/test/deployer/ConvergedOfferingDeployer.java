package org.zstack.test.deployer;

import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.sdk.CreateInstanceOfferingAction;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.ConvergedOfferingConfig;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

public class ConvergedOfferingDeployer implements InstanceOfferingDeployer<ConvergedOfferingConfig> {
    @Override
    public void deploy(List<ConvergedOfferingConfig> offerings, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (ConvergedOfferingConfig ic : offerings) {
            CreateInstanceOfferingAction action = new CreateInstanceOfferingAction();


            SessionInventory session = ic.getAccountRef() == null ? deployer.getApi().getAdminSession() : deployer.loginByAccountRef(ic.getAccountRef(), config);
            action.sessionId = session.getUuid();
            action.allocatorStrategy = ic.getAllocatorStrategy();
            action.cpuNum = (int) ic.getCpuNum();
            action.description = ic.getDescription();
            action.memorySize = deployer.parseSizeCapacity(ic.getMemoryCapacity());
            action.name = ic.getName();
            action.type = ConfigurationConstant.USER_VM_INSTANCE_OFFERING_TYPE;
            action.systemTags = new ArrayList();

            if (ic.getNetworkOutboundBandwidth() != null) {
                action.systemTags.add(String.format("networkOutboundBandwidth::%s", ic.getNetworkOutboundBandwidth()));
            }
            if (ic.getNetworkInboundBandwidth() != null) {
                action.systemTags.add(String.format("networkInboundBandwidth::%s", ic.getNetworkInboundBandwidth()));
            }
            if (ic.getVolumeTotalBandwidth() != null) {
                action.systemTags.add(String.format("volumeTotalBandwidth::%s", ic.getVolumeTotalBandwidth()));
            }
            if (ic.getVolumeTotalIops() != null) {
                action.systemTags.add(String.format("volumeTotalIops::%s", ic.getVolumeTotalIops()));
            }

            CreateInstanceOfferingAction.Result res = action.call();
            res.throwExceptionIfError();

            InstanceOfferingInventory inv = JSONObjectUtil.rehashObject(res.value.getInventory(), InstanceOfferingInventory.class);
            deployer.instanceOfferings.put(inv.getName(), inv);
        }
    }

    @Override
    public Class<ConvergedOfferingConfig> getSupportedDeployerClassType() {
        return ConvergedOfferingConfig.class;
    }
}
