package org.zstack.header.securitymachine.secretresourcepool;

import org.zstack.header.message.NeedReplyMessage;

public class CreateSecretkeyMsg extends NeedReplyMessage implements SecretResourcePoolMessage {
	private String secretResourcePoolUuid;
	private String algorithm;
	private String keyUsage;

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public String getKeyUsage() {
		return keyUsage;
	}

	public void setKeyUsage(String keyUsage) {
		this.keyUsage = keyUsage;
	}

	public void setSecretResourcePoolUuid(String secretResourcePoolUuid) {
		this.secretResourcePoolUuid = secretResourcePoolUuid;
	}

	@Override
	public String getSecretResourcePoolUuid() {
		return secretResourcePoolUuid;
	}
}
