package org.zstack.crypto.securitymachine.thirdparty.westone;


import org.zstack.header.rest.RESTFacade;
import org.zstack.header.securitymachine.SecurityMachineClient;
import org.zstack.header.securitymachine.SecurityMachineResponse;
import org.zstack.opencrypto.securitymachine.AttachVerifyPair;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.InputStream;
import java.security.cert.X509Certificate;

public class WestoneClient implements SecurityMachineClient, AutoCloseable{

	private static final CLogger logger = Utils.getLogger(WestoneClient.class);

	private RESTFacade restf;

	@Override
	public SecurityMachineResponse<String> attachedSignature(String s, String s1) {
		return null;
	}

	@Override
	public SecurityMachineResponse<AttachVerifyPair> attachedVerify(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> digest(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> digestLocal(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<X509Certificate> genericCertificate(byte[] bytes) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> sm4Encrypt(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> sm4Decrypt(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> hmac(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<byte[]> largeFileHmac(InputStream inputStream) {
		return null;
	}

	@Override
	public SecurityMachineResponse<Boolean> connect(String uuid) {
		return null;
	}

	@Override
	public SecurityMachineResponse<Boolean> connect(String ip, int port, String password) {
		return null;
	}

	@Override
	public String getType() {
		return WestoneSecretResourcePoolConstant.SECRET_RESOURCE_POOL_TYPE;
	}

	@Override
	public SecurityMachineResponse<Boolean> isSecretKeyExist(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> generateToken(String s, String s1) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> generateSm4Token(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> generateDataProtectToken(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> generateHmacToken(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> generateToken(String s) {
		return null;
	}

	@Override
	public SecurityMachineResponse<String> backupToken() {
		return null;
	}

	@Override
	public void close() throws Exception {

	}
}
