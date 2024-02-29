package org.zstack.crypto.securitymachine.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
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
public class APICreateWestoneSecretResourcePoolMsg extends APICreateSecretResourcePoolMsg {
	private String tenantId;
	private String appId;
	private String secret;
	@NoLogging(type = NoLogging.Type.Uri)
	private String initParamUrl;
	private String initParamWorkdId;
	private String initParamWorkdir;

	public String getInitParamWorkdir() {
		return initParamWorkdir;
	}

	public void setInitParamWorkdir(String initParamWorkdir) {
		this.initParamWorkdir = initParamWorkdir;
	}

	public String getInitParamUrl() {
		return initParamUrl;
	}

	public void setInitParamUrl(String initParamUrl) {
		this.initParamUrl = initParamUrl;
	}

	public String getInitParamWorkdId() {
		return initParamWorkdId;
	}

	public void setInitParamWorkdId(String initParamWorkdId) {
		this.initParamWorkdId = initParamWorkdId;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
}
