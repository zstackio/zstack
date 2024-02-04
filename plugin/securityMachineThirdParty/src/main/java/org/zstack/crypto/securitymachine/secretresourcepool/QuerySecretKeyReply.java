package org.zstack.crypto.securitymachine.secretresourcepool;

import org.zstack.header.message.APIReply;

public class QuerySecretKeyReply extends APIReply {
	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	private String secretKey;
}
