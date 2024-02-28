package org.zstack.crypto.securitymachine.thirdparty.zhongfu;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.crypto.securitymachine.api.APICreateZhongfuSecretResourcePoolMsg;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.securitymachine.secretresourcepool.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class ZhongfuSecretResourcePoolFactory implements SecretResourcePoolFactory {
	private static final CLogger logger = Utils.getLogger(ZhongfuSecretResourcePoolFactory.class);

	@Autowired
	private DatabaseFacade dbf;

	@Override
	public String getSecretResourcePoolModel() {
		return ZhongfuSecretResourcePoolConstant.SECRET_RESOURCE_POOL_TYPE;
	}

	@Override
	public SecretResourcePoolVO createSecretResourcePool(SecretResourcePoolVO vo, CreateSecretResourcePoolMessage msg) {
		if (!(msg instanceof APICreateZhongfuSecretResourcePoolMsg)) {
			throw new OperationFailureException(Platform.operr("secretResourcePool[uuid:%s] model is not %s", msg.getResourceUuid(), vo.getModel()));
		}
		APICreateZhongfuSecretResourcePoolMsg createZhongfuSecretResourcePoolMessage = (APICreateZhongfuSecretResourcePoolMsg) msg;
		ZhongfuSecretResourcePoolVO ivo = new ZhongfuSecretResourcePoolVO(vo);
		ivo = dbf.persistAndRefresh(ivo);
		return ivo;
	}

	@Override
	public SecretResourcePoolInventory getSecretResourcePoolInventory(String uuid) {
		ZhongfuSecretResourcePoolVO vo = dbf.findByUuid(uuid, ZhongfuSecretResourcePoolVO.class);
		return vo == null ? null : ZhongfuSecretResourcePoolInventory.valueOf(vo);
	}

	@Override
	public SecretResourcePool getSecretResourcePool(SecretResourcePoolVO vo) {
		ZhongfuSecretResourcePoolVO zhongfuSecretResourcePoolVO = dbf.findByUuid(vo.getUuid(), ZhongfuSecretResourcePoolVO.class);
		return new ZhongfuSecretResourcePoolBase(zhongfuSecretResourcePoolVO);
	}
}
