package org.zstack.crypto.securitymachine.secretresourcepool;

import org.zstack.header.securitymachine.secretresourcepool.CreateSecretResourcePoolMessage;

public interface CreateWestoneSecretResourcePoolMessage extends CreateSecretResourcePoolMessage {
	String getTenantId();
	String getAppId();
	String getSecret();
    String getInitParamUrl();
    String getInitParamWorkdId();
	String getInitParamWorkdir();
}
