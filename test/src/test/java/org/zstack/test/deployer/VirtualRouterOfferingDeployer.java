package org.zstack.test.deployer;

import org.zstack.header.configuration.APICreateInstanceOfferingEvent;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.network.service.virtualrouter.APICreateVirtualRouterOfferingMsg;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.deployer.schema.DeployerConfig;
import org.zstack.test.deployer.schema.VirtualRouterOfferingConfig;

import java.util.List;

public class VirtualRouterOfferingDeployer implements InstanceOfferingDeployer<VirtualRouterOfferingConfig> {
	@Override
	public Class<VirtualRouterOfferingConfig> getSupportedDeployerClassType() {
		return VirtualRouterOfferingConfig.class;
	}

	@Override
	public void deploy(List<VirtualRouterOfferingConfig> offerings, DeployerConfig config, Deployer deployer) throws ApiSenderException {
		for (VirtualRouterOfferingConfig ic : offerings) {
			APICreateVirtualRouterOfferingMsg msg = new APICreateVirtualRouterOfferingMsg();
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
            msg.setSession(session);
			msg.setAllocatorStrategy(ic.getAllocatorStrategy());
			msg.setCpuNum((int) ic.getCpuNum());
			msg.setCpuSpeed((int) ic.getCpuSpeed());
			msg.setDescription(ic.getDescription());
			msg.setMemorySize(deployer.parseSizeCapacity(ic.getMemoryCapacity()));
			msg.setName(ic.getName());
			msg.setManagementNetworkUuid(mgmtNw.getUuid());
			msg.setType(VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
			if (publicNw != null) {
				msg.setPublicNetworkUuid(publicNw.getUuid());
			}
			msg.setZoneUuid(zone.getUuid());
			msg.setImageUuid(image.getUuid());
			if (ic.getAccountRef() == null) {
				msg.setDefault(ic.isIsDefault());
			}
			ApiSender sender = deployer.getApi().getApiSender();
			APICreateInstanceOfferingEvent evt = sender.send(msg, APICreateInstanceOfferingEvent.class);
			InstanceOfferingInventory iinv = evt.getInventory();
			deployer.instanceOfferings.put(iinv.getName(), iinv);
		}
	}
}
