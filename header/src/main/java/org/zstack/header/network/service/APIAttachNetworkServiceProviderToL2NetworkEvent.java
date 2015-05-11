package org.zstack.header.network.service;

import org.zstack.header.message.APIEvent;

public class APIAttachNetworkServiceProviderToL2NetworkEvent extends APIEvent {
	private NetworkServiceProviderInventory inventory;

	public APIAttachNetworkServiceProviderToL2NetworkEvent() {
		super(null);
	}
	
	public APIAttachNetworkServiceProviderToL2NetworkEvent(String apiId) {
		super(apiId);
	}
	
	public NetworkServiceProviderInventory getInventory() {
		return inventory;
	}

	public void setInventory(NetworkServiceProviderInventory inventory) {
		this.inventory = inventory;
	}
}
