package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

public class APICreateInstanceOfferingEvent extends APIEvent {
	private InstanceOfferingInventory inventory;
	
	public InstanceOfferingInventory getInventory() {
    	return inventory;
    }

	public void setInventory(InstanceOfferingInventory inventory) {
    	this.inventory = inventory;
    }

	public APICreateInstanceOfferingEvent() {
		super(null);
	}
	
	public APICreateInstanceOfferingEvent(String apiId) {
	    super(apiId);
    }

}
