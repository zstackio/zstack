package org.zstack.network.service.virtualrouter;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.APICreateInstanceOfferingEvent;
import org.zstack.header.configuration.APICreateInstanceOfferingMsg;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneVO;

@Action(category = VirtualRouterConstant.ACTION_CATEGORY)
@TagResourceType(InstanceOfferingVO.class)
@RestRequest(
		path = "/instance-offerings/virtual-routers",
		responseClass = APICreateInstanceOfferingEvent.class,
		parameterName = "params",
		method = HttpMethod.POST
)
public class APICreateVirtualRouterOfferingMsg extends APICreateInstanceOfferingMsg {
	@APIParam(resourceType = ZoneVO.class)
	private String zoneUuid;
	@APIParam(resourceType = L3NetworkVO.class, checkAccount = true)
	private String managementNetworkUuid;
	@APIParam(resourceType = ImageVO.class, checkAccount = true)
	private String imageUuid;
    @APIParam(required = false, resourceType = L3NetworkVO.class, checkAccount = true)
	private String publicNetworkUuid;
	private Boolean isDefault;

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

	public Boolean isDefault() {
		return isDefault;
	}

	public void setDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	public String getImageUuid() {
		return imageUuid;
	}
	public void setImageUuid(String imageUuid) {
		this.imageUuid = imageUuid;
	}
}
