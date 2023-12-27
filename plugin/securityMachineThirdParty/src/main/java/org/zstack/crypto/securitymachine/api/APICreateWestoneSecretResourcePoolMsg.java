package org.zstack.crypto.securitymachine.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.securitymachine.secretresourcepool.CreateWestoneSecretResourcePoolMessage;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.securitymachine.api.secretresourcepool.APICreateSecretResourcePoolEvent;
import org.zstack.header.securitymachine.api.secretresourcepool.APICreateSecretResourcePoolMsg;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;
import org.zstack.header.tag.TagResourceType;

import java.util.concurrent.TimeUnit;

@RestRequest(path = "/secret-resource-pool/westone", method = HttpMethod.POST, parameterName = "params", responseClass = APICreateSecretResourcePoolEvent.class)
@TagResourceType(SecretResourcePoolVO.class)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APICreateWestoneSecretResourcePoolMsg extends APICreateSecretResourcePoolMsg implements CreateWestoneSecretResourcePoolMessage {
	private String tenantId;
	private String appId;
	private String secret;
	private String initParamUrl;
	private String initParamWorkdId;
	private String initParamWorkdir;

	@Override
	public String getInitParamWorkdir() {
		return initParamWorkdir;
	}

	public void setInitParamWorkdir(String initParamWorkdir) {
		this.initParamWorkdir = initParamWorkdir;
	}

	@Override
	public String getInitParamUrl() {
		return initParamUrl;
	}

	public void setInitParamUrl(String initParamUrl) {
		this.initParamUrl = initParamUrl;
	}

	@Override
	public String getInitParamWorkdId() {
		return initParamWorkdId;
	}

	public void setInitParamWorkdId(String initParamWorkdId) {
		this.initParamWorkdId = initParamWorkdId;
	}

	@Override
	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	@Override
	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
}
