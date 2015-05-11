package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.APIParam;
import org.zstack.header.vm.APICreateVmInstanceMsg;

import java.util.Set;

public class APICreateVirtualRouterVmMsg extends APICreateVmInstanceMsg {
	@APIParam
    private String managementNetworkUuid;
	@APIParam
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
