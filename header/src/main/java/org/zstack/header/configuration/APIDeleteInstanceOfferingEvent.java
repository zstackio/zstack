package org.zstack.header.configuration;

import org.zstack.header.message.APIEvent;

public class APIDeleteInstanceOfferingEvent extends APIEvent {
	public APIDeleteInstanceOfferingEvent() {
		super(null);
	}
	
	public APIDeleteInstanceOfferingEvent(String apiId) {
	    super(apiId);
    }
}
