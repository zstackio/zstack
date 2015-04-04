package org.zstack.header.configuration;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

public class APIDeleteDiskOfferingMsg extends APIDeleteMessage implements DiskOfferingMessage {
	@APIParam
	private String uuid;

	public APIDeleteDiskOfferingMsg() {
	}
	
	public APIDeleteDiskOfferingMsg(String uuid) {
	    super();
	    this.uuid = uuid;
    }

	public String getUuid() {
    	return uuid;
    }

	public void setUuid(String uuid) {
    	this.uuid = uuid;
    }

    @Override
    public String getDiskOfferingUuid() {
        return uuid;
    }
}
