package org.zstack.crypto.securitymachine.thirdparty.zhongfu;

import org.zstack.core.Platform;
import org.zstack.crypto.securitymachine.api.APICreateZhongfuSecretResourcePoolMsg;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;

public class ZhongfuSecretResourcePoolApiInterceptor implements ApiMessageInterceptor {
	@Override
	public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
		if (msg instanceof APICreateZhongfuSecretResourcePoolMsg){
			validate((APICreateZhongfuSecretResourcePoolMsg)msg);
		}
		return msg;
	}

	private void validate(APICreateZhongfuSecretResourcePoolMsg msg) {
		if (!msg.getModel().equals(ZhongfuSecurityMachineConstant.SECURITY_MACHINE_TYPE))
			throw new ApiMessageInterceptionException(Platform.argerr("currently does not support the creation of %s resource pools", msg.getModel()));
	}
}
