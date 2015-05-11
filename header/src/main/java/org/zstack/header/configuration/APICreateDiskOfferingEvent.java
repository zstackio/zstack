package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

public class APICreateDiskOfferingEvent extends APIEvent {
	private DiskOfferingInventory inventory;

	public APICreateDiskOfferingEvent() {
		super(null);
	}
	
	public APICreateDiskOfferingEvent(String apiId) {
	    super(apiId);
    }

	public DiskOfferingInventory getInventory() {
    	return inventory;
    }

	public void setInventory(DiskOfferingInventory inventory) {
    	this.inventory = inventory;
    }
	
}
