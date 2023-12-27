package org.zstack.crypto.securitymachine.thirdparty.westone;

import org.zstack.core.Platform;
import org.zstack.crypto.securitymachine.api.APICreateWestoneSecretResourcePoolMsg;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.message.APIMessage;

public class WestoneSecretResourcePoolApiInterceptor implements ApiMessageInterceptor {
	@Override
	public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
		if (msg instanceof APICreateWestoneSecretResourcePoolMsg){
			validate((APICreateWestoneSecretResourcePoolMsg)msg);
		}
		return msg;
	}

	private void validate(APICreateWestoneSecretResourcePoolMsg msg) {
		if (!msg.getModel().equals(WestoneSecurityMachineConstant.SECURITY_MACHINE_TYPE))
			throw new ApiMessageInterceptionException(Platform.argerr("currently does not support the creation of %s resource pools", msg.getModel()));
	}
}
