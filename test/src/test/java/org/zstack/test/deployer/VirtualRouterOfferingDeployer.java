package org.zstack.test.deployer;

import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.sdk.CreateVirtualRouterOfferingAction;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.VirtualRouterOfferingConfig;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

public class VirtualRouterOfferingDeployer implements InstanceOfferingDeployer<VirtualRouterOfferingConfig> {
    @Override
    public Class<VirtualRouterOfferingConfig> getSupportedDeployerClassType() {
        return VirtualRouterOfferingConfig.class;
    }

    @Override
    public void deploy(List<VirtualRouterOfferingConfig> offerings, DeployerConfig config, Deployer deployer) throws ApiSenderException {
        for (VirtualRouterOfferingConfig ic : offerings) {

            CreateVirtualRouterOfferingAction action = new CreateVirtualRouterOfferingAction();

            L3NetworkInventory mgmtNw = deployer.l3Networks.get(ic.getManagementL3NetworkRef());
            if (mgmtNw == null) {
                throw new CloudRuntimeException(String.format("unable to find l3network[%s]", ic.getManagementL3NetworkRef()));
            }
            L3NetworkInventory publicNw = null;
            if (ic.getPublicL3NetworkRef() != null) {
                publicNw = deployer.l3Networks.get(ic.getPublicL3NetworkRef());
                if (publicNw == null) {
                    throw new CloudRuntimeException(String.format("unable to find l3network[%s]", ic.getManagementL3NetworkRef()));
                }
            }
            ImageInventory image = deployer.images.get(ic.getImageRef());
            if (image == null) {
                throw new CloudRuntimeException(String.format("unable to find image[%s]", ic.getImageRef()));
            }
            ZoneInventory zone = deployer.zones.get(ic.getZoneRef());
            if (zone == null) {
                throw new CloudRuntimeException(String.format("unable to find zone[%s]", ic.getZoneRef()));
            }

            SessionInventory session = ic.getAccountRef() == null ? deployer.getApi().getAdminSession() : deployer.loginByAccountRef(ic.getAccountRef(), config);

            action.sessionId = session.getUuid();
            action.allocatorStrategy = ic.getAllocatorStrategy();
            action.cpuNum = (int) ic.getCpuNum();
            action.cpuSpeed = (int) ic.getCpuSpeed();
            action.description = ic.getDescription();
            action.memorySize = deployer.parseSizeCapacity(ic.getMemoryCapacity());
            action.name = ic.getName();
            action.managementNetworkUuid = mgmtNw.getUuid();
            action.type = VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE;

            if (publicNw != null) {
                action.publicNetworkUuid = publicNw.getUuid();
            }

            action.zoneUuid = zone.getUuid();
            action.imageUuid = image.getUuid();

            if (ic.getAccountRef() == null) {
                action.isDefault = ic.isIsDefault();
            }

            CreateVirtualRouterOfferingAction.Result res = action.call().throwExceptionIfError();
            InstanceOfferingInventory iinv = JSONObjectUtil.rehashObject(res.value.getInventory(), InstanceOfferingInventory.class);
            deployer.instanceOfferings.put(iinv.getName(), iinv);
        }
    }
}
