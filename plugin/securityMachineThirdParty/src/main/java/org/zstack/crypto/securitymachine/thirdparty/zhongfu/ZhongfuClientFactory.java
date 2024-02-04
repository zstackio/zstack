package org.zstack.crypto.securitymachine.thirdparty.zhongfu;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.securitymachine.AddSecurityMachineMessage;
import org.zstack.header.securitymachine.SecurityMachineClient;
import org.zstack.header.securitymachine.SecurityMachineClientFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ZhongfuClientFactory implements SecurityMachineClientFactory {
	private static final CLogger logger = Utils.getLogger(ZhongfuClientFactory.class);

	@Override
	public SecurityMachineClient create() {
		return new ZhongfuClient();
	}

	@Override
	public String getType() {
		return ZhongfuSecurityMachineConstant.SECURITY_MACHINE_TYPE;
	}

	@Override
	public ErrorCode testConnection(AddSecurityMachineMessage message) {
		return null;
	}
}
