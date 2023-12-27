package org.zstack.crypto.securitymachine.secretresourcepool;

import org.zstack.header.message.MessageReply;

public class CreateSecretkeyReply extends MessageReply {
	private String keyId;
	private String secretKey;
	public CreateSecretkeyReply(){}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
}
