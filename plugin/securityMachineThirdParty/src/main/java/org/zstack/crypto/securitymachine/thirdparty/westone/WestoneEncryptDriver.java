package org.zstack.crypto.securitymachine.thirdparty.westone;

import org.zstack.core.encrypt.EncryptDriver;
import org.zstack.core.encrypt.EncryptDriverType;
import org.zstack.core.encrypt.EncryptFacadeResult;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class WestoneEncryptDriver implements EncryptDriver {
	private static final CLogger logger = Utils.getLogger(WestoneEncryptDriver.class);
	@Override
	public EncryptDriverType getDriverType() {
		return new EncryptDriverType(WestoneSecurityMachineConstant.SECURITY_MACHINE_TYPE);
	}

	@Override
	public String encrypt(String data) {
		return null;
	}

	@Override
	public String decrypt(String data) {
		return null;
	}

	@Override
	public EncryptFacadeResult<String> encrypt(String data, String algType) {
		return null;
	}

	@Override
	public EncryptFacadeResult<String> decrypt(String data, String algType) {
		return null;
	}
}
