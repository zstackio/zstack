package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

public class APIDeleteDiskOfferingEvent extends APIEvent {
	public APIDeleteDiskOfferingEvent() {
		super(null);
	}
	
	public APIDeleteDiskOfferingEvent(String apiId) {
	    super(apiId);
    }
}
