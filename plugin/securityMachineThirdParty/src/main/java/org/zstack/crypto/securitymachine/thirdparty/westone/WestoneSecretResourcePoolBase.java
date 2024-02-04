package org.zstack.crypto.securitymachine.thirdparty.westone;

import cn.com.westone.wcspsdk.baseservice.co.COService;
import cn.com.westone.wcspsdk.baseservice.km.KMService;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.sugon.westone.WcspContext;
import org.sugon.westone.WestoneBase;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.crypto.securitymachine.api.APIWestoneTestMsg;
import org.zstack.crypto.securitymachine.secretresourcepool.CreateSecretkeyReply;
import org.zstack.crypto.securitymachine.secretresourcepool.DeleteSecretKeyReply;
import org.zstack.crypto.securitymachine.secretresourcepool.QuerySecretKeyReply;
import org.zstack.crypto.securitymachine.secretresourcepool.TPSecretResourcePoolConstant;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.securitymachine.secretresourcepool.*;
import org.zstack.opencrypto.securitymachine.SecurityMachineGlobalConfig;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class WestoneSecretResourcePoolBase implements SecretResourcePool {

	private static final CLogger logger = Utils.getLogger(WestoneSecretResourcePoolBase.class);
	@Autowired
	protected CloudBus bus;
	@Autowired
	protected DatabaseFacade dbf;
	protected SecretResourcePoolVO self;
	protected final String id;

	private WcspContext wcspContext;

	public WcspContext getWcspContext() {
		return wcspContext;
	}

	public void setWcspContext(WcspContext wcspContext) {
		this.wcspContext = wcspContext;
	}


	protected WestoneSecretResourcePoolBase(SecretResourcePoolVO self) {
		this.self = self;
		this.id = SecretResourcePool.buildId(self.getUuid());
	}


	protected SecretResourcePoolInventory getSelfInventory() {
		return WestoneSecretResourcePoolInventory.valueOf(getSelf());
	}

	@Override
	public void handleMessage(Message msg) {
		if (msg instanceof APIMessage) {
			handleApiMessage((APIMessage)msg);
		} else {
			handleLocalMessage(msg);
		}
	}

	@Override
	public String getId() {
		return null;
	}

	protected void handleApiMessage(APIMessage msg) {
		if (msg instanceof APIWestoneTestMsg) {
			handle((APIWestoneTestMsg) msg);
		}
	}

	protected void handleLocalMessage(Message msg) {
		if (msg instanceof CreateSecretkeyMsg) {
			handle((CreateSecretkeyMsg) msg);
		} else if (msg instanceof QuerySecretKeyMsg){
			handle((QuerySecretKeyMsg) msg);
		} else if (msg instanceof DeleteSecretkeyMsg){
			handle((DeleteSecretkeyMsg) msg);
		}
	}

	private void handle(APIWestoneTestMsg apiWestoneTestMsg) {
		String resourceId = SecurityMachineGlobalConfig.RESOURCE_POOL_UUID_FOR_DATA_PROTECT.value(String.class);
		if(apiWestoneTestMsg.getMsgType().equals("create")) {
			//创建
			CreateSecretkeyMsg msg = new CreateSecretkeyMsg();
			//传入参数
			msg.setSecretResourcePoolUuid(resourceId);
			msg.setAlgorithm(TPSecretResourcePoolConstant.ALGORITHM_SM4);
			msg.setKeyUsage(TPSecretResourcePoolConstant.KEY_USAGE_ENC);
			bus.makeTargetServiceIdByResourceUuid(msg, SecretResourcePoolConstant.SERVICE_ID, resourceId);
			bus.send(msg, new CloudBusCallBack(msg) {
				@Override
				public void run(MessageReply reply) {
					//返回值
					CreateSecretkeyReply cr = reply.castReply();
					logger.info("创建密钥keyId=" + cr.getKeyId());
					logger.info("创建密钥secretKey=" + cr.getSecretKey());
					logger.info("创建密钥isSuccess=" + reply.isSuccess());
					if (!reply.isSuccess()) {
						logger.info("创建密钥error=" + reply.getError()
								.getDetails());
					}
				}
			});
		} else if(apiWestoneTestMsg.getMsgType().equals("query")) {
			//查询
			QuerySecretKeyMsg qMsg = new QuerySecretKeyMsg();
			qMsg.setKeyId(apiWestoneTestMsg.getKeyId());
			qMsg.setSecretResourcePoolUuid(resourceId);
			bus.makeTargetServiceIdByResourceUuid(qMsg, SecretResourcePoolConstant.SERVICE_ID, resourceId);
			bus.send(qMsg, new CloudBusCallBack(qMsg) {
				@Override
				public void run(MessageReply reply) {
					//返回值
					QuerySecretKeyReply cr = reply.castReply();
					logger.info("查询密钥secretKey=" + cr.getSecretKey());
					logger.info("查询密钥isSuccess=" + reply.isSuccess());
					if (!reply.isSuccess()) {
						logger.info("查询密钥error=" + reply.getError()
								.getDetails());
					}
				}
			});
		}else if(apiWestoneTestMsg.getMsgType().equals("delete")) {
			//删除
			DeleteSecretkeyMsg dMsg = new DeleteSecretkeyMsg();
			dMsg.setKeyId(apiWestoneTestMsg.getKeyId());
			dMsg.setSecretResourcePoolUuid(resourceId);
			bus.makeTargetServiceIdByResourceUuid(dMsg, SecretResourcePoolConstant.SERVICE_ID, resourceId);
			bus.send(dMsg, new CloudBusCallBack(dMsg) {
				@Override
				public void run(MessageReply reply) {
					//返回值
					logger.info("删除密钥isSuccess=" + reply.isSuccess());
					if (!reply.isSuccess()) {
						logger.info("删除密钥error=" + reply.getError()
								.getDetails());
					}
				}
			});
		}
	}

	private void handle(DeleteSecretkeyMsg msg) {
		DeleteSecretKeyReply reply = new DeleteSecretKeyReply();
		COService coService = null;
		try{
			String keyId=msg.getKeyId();
			coService = wcspContext.getCOService();
			//密钥管理服务初始化
			WestoneBase westoneBase = new WestoneBase();
			KMService kmService=westoneBase.initWestoneKMService(coService.platform());
			westoneBase.deleteSecretkey(kmService,keyId);
			reply.setSuccess(true);
		} catch (Exception e) {
			reply.setSuccess(false);
			reply.setError(Platform.err(
					SysErrors.CREATE_RESOURCE_ERROR,e.getMessage()));
		}finally {
			if (coService != null) {
				wcspContext.returnCOService(coService);
			}
		}
		bus.reply(msg, reply);
	}

	private void handle(QuerySecretKeyMsg msg) {
		String keyId = msg.getKeyId();
		QuerySecretKeyReply reply = new QuerySecretKeyReply();
		COService coService = null;
		try{
			coService = wcspContext.getCOService();
			WestoneBase westoneBase = new WestoneBase();
			KMService kmService=westoneBase.initWestoneKMService(coService.platform());
			reply.setSecretKey(westoneBase.querySecretKey(kmService,coService,keyId));
			reply.setSuccess(true);
		} catch(Exception e){
			reply.setSuccess(false);
			reply.setError(Platform.err(SysErrors.CREATE_RESOURCE_ERROR, e.getMessage()));
		}  finally {
			if (coService != null) {
				wcspContext.returnCOService(coService);
			}
		}
		bus.reply(msg,reply);
	}

	private void handle(CreateSecretkeyMsg msg) {
		CreateSecretkeyReply reply = new CreateSecretkeyReply();
		String algorithm=msg.getAlgorithm();
		String keyUsage=msg.getKeyUsage();
		COService coService = null;
		try{
			coService = wcspContext.getCOService();
			WestoneBase westoneBase = new WestoneBase();
			KMService kmService=westoneBase.initWestoneKMService(coService.platform());
			Map<String,String> map = westoneBase.createSecretkey(kmService,coService,algorithm,keyUsage);
			reply.setSecretKey(map.get("data"));
			reply.setKeyId(map.get("keyId"));
			reply.setSuccess(true);
		} catch (Exception e) {
			e.printStackTrace();
			reply.setSuccess(false);
			reply.setError(Platform.err(SysErrors.CREATE_RESOURCE_ERROR,e.getMessage()));
		} finally {
			if (coService != null) {
				wcspContext.returnCOService(coService);
			}
		}
		bus.reply(msg, reply);
	}
	private WestoneSecretResourcePoolVO getSelf() {
		return (WestoneSecretResourcePoolVO) this.self;
	}

}
