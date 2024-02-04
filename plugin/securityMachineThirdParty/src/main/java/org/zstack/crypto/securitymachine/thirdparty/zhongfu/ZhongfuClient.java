package org.zstack.crypto.securitymachine.thirdparty.zhongfu;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.ccs.CCSCertificateUserRefVO;
import org.zstack.header.ccs.CCSCertificateUserRefVO_;
import org.zstack.header.ccs.CCSCertificateVO;
import org.zstack.header.ccs.CCSCertificateVO_;
import org.zstack.header.securitymachine.SecurityMachineClient;
import org.zstack.header.securitymachine.SecurityMachineResponse;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.opencrypto.securitymachine.AttachVerifyPair;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

public class ZhongfuClient implements SecurityMachineClient, AutoCloseable{

	private static final CLogger logger = Utils.getLogger(ZhongfuClient.class);

	@Override
	public SecurityMachineResponse<String> attachedSignature(String s, String s1) {
		return null;
	}

	@Override
	public SecurityMachineResponse<AttachVerifyPair> attachedVerify(String cipherText) {
		if (cipherText == null) {
			String errorText = "attachedVerify input is null";
			logger.warn(errorText);
			return new SecurityMachineResponse(Platform.operr(errorText));
		} else {
			//证书解析，通过传入的sn号，去查询useruuid、resourceType、到期时间，拼接成
			//userUuid::resourceType::time
			Date now = new Date();
			Long time=now.getTime()-200000L;

			String certificateUuid = Q.New(CCSCertificateVO.class)
					.eq(CCSCertificateVO_.subjectDN, cipherText)
					.select(ResourceVO_.uuid).findValue();
			String userUuid = Q.New(CCSCertificateUserRefVO.class)
					.eq(CCSCertificateUserRefVO_.certificateUuid, certificateUuid)
					.select(CCSCertificateUserRefVO_.userUuid).findValue();

			String resourceType = Q.New(ResourceVO.class)
					.eq(ResourceVO_.uuid, userUuid)
					.select(ResourceVO_.resourceType).findValue();

			String certificateValue=userUuid+"::"+resourceType+"::"+time;

			return AttachVerifyPair.create(certificateValue.getBytes(), certificateValue.getBytes());
		}
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
	public SecurityMachineResponse<X509Certificate> genericCertificate(byte[] cert) {
		Date today = new Date();//获取今天的日期
		Calendar c = Calendar.getInstance();
		c.setTime(today);
		c.add(Calendar.DAY_OF_MONTH, 15);
		Date tomorrow = c.getTime();//两天后
		c.setTime(today);
		c.add(Calendar.DAY_OF_MONTH, -15);
		Date yesterday = c.getTime();//两天前

		//默认为创建AddCCSCertificate length==1
		String[] certArr = new String(cert).split("::");
		String issuerDN = certArr[0];
		String subjectDN = certArr[0];

		//登录length==3 传入格式为 userUuid::resourceType::time 登录时用不到subjectDN
		if(certArr.length==3){
			String userUuid=certArr[0];
			//根据userUuid查询snNum
			String certificateUuid = Q.New(CCSCertificateUserRefVO.class)
					.eq(CCSCertificateUserRefVO_.userUuid, userUuid)
					.select(CCSCertificateUserRefVO_.certificateUuid).findValue();

			subjectDN = Q.New(CCSCertificateVO.class)
					.eq(CCSCertificateVO_.uuid, certificateUuid)
					.select(CCSCertificateVO_.subjectDN).findValue();
			issuerDN = subjectDN;
		}
		X509Certificate certificate = FakeX509Certificate.builder()
				.algorithm("algorithm")
				.issuerDN(issuerDN)//发行者，将snNum set到issuerDN原因是：闭源代码查询时，使用issuerDN和serialNumber进行唯一结果查询
				.subjectDN(subjectDN)//主题
				.serialNumber(1)
				.effectiveTime(yesterday.getTime())//开始时间
				.expirationTime(tomorrow.getTime())//结束时间
				.build();
		return new SecurityMachineResponse(certificate);
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
		logger.info(String.format("invoke connect(uuid=%s)", uuid));
		return new SecurityMachineResponse(true);
	}

	@Override
	public SecurityMachineResponse<Boolean> connect(String ip, int port, String password) {
		return null;
	}

	@Override
	public String getType() {
		return ZhongfuSecretResourcePoolConstant.SECRET_RESOURCE_POOL_TYPE;
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
