package org.zstack.crypto.securitymachine.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolMessage;

import java.util.concurrent.TimeUnit;

@Action(category = "secretResourcePool")
@RestRequest(path = "/secret-resource-pool/westonetest", method = HttpMethod.POST, parameterName = "params", responseClass = APIWestoneTestEvent.class)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APIWestoneTestMsg extends APIMessage implements SecretResourcePoolMessage {

	private String keyId;

	private String uuid;

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	private String msgType;

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	@Override
	public String getSecretResourcePoolUuid() {
		return uuid;
	}
}
