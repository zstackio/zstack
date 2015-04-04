package org.zstack.header.image;

import org.zstack.header.message.APIEvent;

public class APIDeleteImageEvent extends APIEvent {

	public APIDeleteImageEvent(String apiId) {
	    super(apiId);
    }
	
	public APIDeleteImageEvent() {
	    super(null);
	}

}
