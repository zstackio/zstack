package org.zstack.network.service.virtualrouter;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.APICreateVmInstanceEvent;
import org.zstack.header.vm.APICreateVmInstanceMsg;

import java.util.Set;

@RestRequest(
		path = "/vm-instances/appliances/virtual-routers",
		method = HttpMethod.POST,
		responseClass = APICreateVmInstanceEvent.class,
		parameterName = "params"
)
public class APICreateVirtualRouterVmMsg extends APICreateVmInstanceMsg {
	@APIParam(resourceType = L3NetworkVO.class, checkAccount = true)
    private String managementNetworkUuid;
	@APIParam(resourceType = L3NetworkVO.class, checkAccount = true)
    private String publicNetworkUuid;
	@APIParam
	private Set<String> networkServicesProvided;
	
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
	public Set<String> getNetworkServicesProvided() {
		return networkServicesProvided;
	}
	public void setNetworkServicesProvided(Set<String> networkServicesProvided) {
		this.networkServicesProvided = networkServicesProvided;
	}
	
}
