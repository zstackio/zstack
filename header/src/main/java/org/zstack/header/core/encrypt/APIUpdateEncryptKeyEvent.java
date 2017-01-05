package org.zstack.header.core.encrypt;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 16/12/28.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateEncryptKeyEvent extends APIEvent {

	public APIUpdateEncryptKeyEvent() {
	super(null);
}

	public APIUpdateEncryptKeyEvent(String apiId) {
		super(apiId);
	}

}
