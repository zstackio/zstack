package org.zstack.crypto.securitymachine.thirdparty.zhongfu;

import org.zstack.core.encrypt.EncryptDriver;
import org.zstack.core.encrypt.EncryptDriverType;
import org.zstack.core.encrypt.EncryptFacadeResult;
import org.zstack.core.encrypt.EncryptRSA;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ZhongfuEncryptDriver implements EncryptDriver {
	private static final CLogger logger = Utils.getLogger(ZhongfuEncryptDriver.class);
	public static EncryptRSA rsa = new EncryptRSA();
	@Override
	public EncryptDriverType getDriverType() {
		return new EncryptDriverType(ZhongfuSecurityMachineConstant.SECURITY_MACHINE_TYPE);
	}

	@Override
	public String encrypt(String data) {
		try {
			return rsa.encrypt1(data);
		} catch (Exception e) {
			throw new CloudRuntimeException(e.getMessage());
		}
	}

	@Override
	public String decrypt(String data) {
		try {
			return (String) rsa.decrypt1(data);
		} catch (Exception e) {
			throw new CloudRuntimeException(e.getMessage());
		}
	}
	@Override
	public EncryptFacadeResult<String> encrypt(String data, String algType) {
		try {
			return new EncryptFacadeResult<>(rsa.encrypt(data, algType));
		} catch (Exception e) {
			throw new CloudRuntimeException(e.getMessage());
		}
	}

	@Override
	public EncryptFacadeResult<String> decrypt(String data, String algType) {
		try {
			return new EncryptFacadeResult<>(rsa.decrypt(data, algType));
		} catch (Exception e) {
			throw new CloudRuntimeException(e.getMessage());
		}
	}
}
