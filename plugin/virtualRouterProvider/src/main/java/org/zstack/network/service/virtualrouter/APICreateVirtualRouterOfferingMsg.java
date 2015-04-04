package org.zstack.network.service.virtualrouter;

import org.zstack.header.configuration.APICreateInstanceOfferingMsg;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.zone.ZoneVO;

public class APICreateVirtualRouterOfferingMsg extends APICreateInstanceOfferingMsg {
	@APIParam(resourceType = ZoneVO.class)
	private String zoneUuid;
	@APIParam(resourceType = L3NetworkVO.class)
	private String managementNetworkUuid;
	@APIParam(resourceType = ImageVO.class)
	private String imageUuid;
    @APIParam(required = false, resourceType = L3NetworkVO.class)
	private String publicNetworkUuid;
	private boolean isDefault;

    public String getType() {
        return VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE;
    }

	public String getZoneUuid() {
		return zoneUuid;
	}
	public void setZoneUuid(String zoneUuid) {
		this.zoneUuid = zoneUuid;
	}
	public String getManagementNetworkUuid() {
		return managementNetworkUuid;
	}
	public void setManagementNetworkUuid(String managementNetworkUuid) {
		this.managementNetworkUuid = managementNetworkUuid;
	}
	public String getPublicNetworkUuid() {
		return publicNetworkUuid;
	}
	public void setPublicNetworkUuid(String publicNetworkUuid) {
		this.publicNetworkUuid = publicNetworkUuid;
	}
	public boolean isDefault() {
		return isDefault;
	}
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}
	public String getImageUuid() {
		return imageUuid;
	}
	public void setImageUuid(String imageUuid) {
		this.imageUuid = imageUuid;
	}
}
