package org.zstack.crypto.securitymachine.thirdparty.westone;

import org.zstack.core.Platform;
import org.zstack.crypto.securitymachine.AddWestoneSecurityMachineMessage;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.securitymachine.AddSecurityMachineMessage;
import org.zstack.header.securitymachine.SecurityMachineClient;
import org.zstack.header.securitymachine.SecurityMachineClientFactory;
import org.zstack.header.securitymachine.SecurityMachineResponse;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class WestoneClientFactory implements SecurityMachineClientFactory {
	private static final CLogger logger = Utils.getLogger(WestoneClientFactory.class);

	@Override
	public SecurityMachineClient create() {
		return new WestoneClient();
	}

	@Override
	public String getType() {
		return WestoneSecurityMachineConstant.SECURITY_MACHINE_TYPE;
	}

	@Override
	public ErrorCode testConnection(AddSecurityMachineMessage message) {
		AddWestoneSecurityMachineMessage westoneMsg = (AddWestoneSecurityMachineMessage) message;
		try (SecurityMachineClient agent = create()){
			SecurityMachineResponse<Boolean> res = agent.connect(westoneMsg.getManagementIp(),westoneMsg.getPort(),westoneMsg.getPassword());
			if(res.getError()!=null){
				return  res.getError();
			}
		} catch (Exception ignored) {
			return Platform.operr(ignored.getMessage(), "WestoneSecClientFactory testConnection failed");
		}
		return null;
	}
}
