package org.zstack.header.securitymachine.secretresourcepool;

import org.zstack.header.message.NeedReplyMessage;

public class DeleteSecretkeyMsg extends NeedReplyMessage implements SecretResourcePoolMessage {
	private String secretResourcePoolUuid;
	private String keyId;

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public void setSecretResourcePoolUuid(String secretResourcePoolUuid) {
		this.secretResourcePoolUuid = secretResourcePoolUuid;
	}

	@Override
	public String getSecretResourcePoolUuid() {
		return secretResourcePoolUuid;
	}
}
