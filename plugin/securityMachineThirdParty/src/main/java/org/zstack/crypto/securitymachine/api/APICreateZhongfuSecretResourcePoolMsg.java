package org.zstack.crypto.securitymachine.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.securitymachine.api.secretresourcepool.APICreateSecretResourcePoolEvent;
import org.zstack.header.securitymachine.api.secretresourcepool.APICreateSecretResourcePoolMsg;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;
import org.zstack.header.tag.TagResourceType;

import java.util.concurrent.TimeUnit;

@RestRequest(path = "/secret-resource-pool/zhongfu", method = HttpMethod.POST, parameterName = "params", responseClass = APICreateSecretResourcePoolEvent.class)
@TagResourceType(SecretResourcePoolVO.class)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APICreateZhongfuSecretResourcePoolMsg extends APICreateSecretResourcePoolMsg {
	private String snNum;

	public String getSnNum() {
		return snNum;
	}

	public void setSnNum(String snNum) {
		this.snNum = snNum;
	}
}
