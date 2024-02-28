package org.zstack.crypto.securitymachine.thirdparty.westone;

import org.springframework.beans.factory.annotation.Autowired;
import org.sugon.westone.WcspContext;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.crypto.securitymachine.api.APICreateWestoneSecretResourcePoolMsg;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.securitymachine.secretresourcepool.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WestoneSecretResourcePoolFactory implements SecretResourcePoolFactory , Component {
	private static final CLogger logger = Utils.getLogger(WestoneSecretResourcePoolFactory.class);

	@Autowired
	private DatabaseFacade dbf;

	private static Map<String, WcspContext> wcspContextMaps = new HashMap<String, WcspContext>();

	@Override
	public String getSecretResourcePoolModel() {
		return WestoneSecretResourcePoolConstant.SECRET_RESOURCE_POOL_TYPE;
	}

	@Override
	public SecretResourcePoolVO createSecretResourcePool(SecretResourcePoolVO vo, CreateSecretResourcePoolMessage msg) {
		if (!(msg instanceof APICreateWestoneSecretResourcePoolMsg)) {
			throw new OperationFailureException(Platform.operr("secretResourcePool[uuid:%s] model is not %s", msg.getResourceUuid(), vo.getModel()));
		}
		APICreateWestoneSecretResourcePoolMsg createWestoneSecretResourcePoolMessage = (APICreateWestoneSecretResourcePoolMsg)msg;
		WestoneSecretResourcePoolVO ivo = new WestoneSecretResourcePoolVO(vo);
		ivo.setAppId(createWestoneSecretResourcePoolMessage.getAppId());
		ivo.setSecret(createWestoneSecretResourcePoolMessage.getSecret());
		ivo.setTenantId(createWestoneSecretResourcePoolMessage.getTenantId());
		ivo.setInitParamUrl(createWestoneSecretResourcePoolMessage.getInitParamUrl());
		ivo.setInitParamWorkdId(createWestoneSecretResourcePoolMessage.getInitParamWorkdId());
		ivo.setInitParamWorkdir(createWestoneSecretResourcePoolMessage.getInitParamWorkdir());
		ivo = dbf.persistAndRefresh(ivo);
		return ivo;
	}

	@Override
	public SecretResourcePoolInventory getSecretResourcePoolInventory(String uuid) {
		WestoneSecretResourcePoolVO vo = dbf.findByUuid(uuid, WestoneSecretResourcePoolVO.class);
		return vo == null ? null : WestoneSecretResourcePoolInventory.valueOf(vo);
	}

	@Override
	public SecretResourcePool getSecretResourcePool(SecretResourcePoolVO vo) {
		WestoneSecretResourcePoolVO westoneSRP = dbf.findByUuid(vo.getUuid(), WestoneSecretResourcePoolVO.class);
		WestoneSecretResourcePoolBase westoneSecretResourcePoolBase = new WestoneSecretResourcePoolBase(westoneSRP);
		WcspContext wcspContext = wcspContextMaps.get(westoneSRP.getAppId());
		westoneSecretResourcePoolBase.setWcspContext(wcspContext);
		return westoneSecretResourcePoolBase;
	}

	@Override
	public boolean start() {
		SimpleQuery<WestoneSecretResourcePoolVO> q = dbf.createQuery(WestoneSecretResourcePoolVO.class);
		q.add(WestoneSecretResourcePoolVO_.model, SimpleQuery.Op.EQ, WestoneSecretResourcePoolConstant.SECRET_RESOURCE_POOL_TYPE);
		//TODO 此处代码需要优化
		List<WestoneSecretResourcePoolVO> westoneSecretResourcePoolList = q.list();
		if(westoneSecretResourcePoolList.size() > 0){
			WestoneSecretResourcePoolVO westoneSRP = westoneSecretResourcePoolList.get(0);
			String url = westoneSRP.getInitParamUrl();
			//密钥管理系统独立部署默认租户ID "default"
			String tenantId = westoneSRP.getTenantId();
			//密钥管理系统，应用管理详情页面获取
			String appId = westoneSRP.getAppId();
			//密钥管理系统，应用管理详情页面获取
			String secret = westoneSRP.getSecret();
			//卫士通需要的目录
			String initParamWorkdir = westoneSRP.getInitParamWorkdir();
			WcspContext wcspContext = new WcspContext();
			wcspContext.setTenantId(tenantId);
			wcspContext.setAppSecret(secret);
			wcspContext.setUrl(url);
			wcspContext.setWorkdir(initParamWorkdir);
			wcspContext.setAppId(appId);
			wcspContext.setMaxPoolSize(200);
			wcspContext.initCoServicePool();
			wcspContextMaps.put(appId, wcspContext);
		}
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}
}
