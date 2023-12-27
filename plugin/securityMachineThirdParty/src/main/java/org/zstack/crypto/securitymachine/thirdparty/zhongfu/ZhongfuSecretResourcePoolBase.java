package org.zstack.crypto.securitymachine.thirdparty.zhongfu;


import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePool;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolInventory;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;


public class ZhongfuSecretResourcePoolBase implements SecretResourcePool {

	private static final CLogger logger = Utils.getLogger(ZhongfuSecretResourcePoolBase.class);
	protected SecretResourcePoolVO self;
	protected final String id;

	protected ZhongfuSecretResourcePoolBase(SecretResourcePoolVO self) {
		this.self = self;
		this.id = SecretResourcePool.buildId(self.getUuid());
	}

	protected SecretResourcePoolInventory getSelfInventory() {
		return null;
	}


	@Override
	public void handleMessage(Message msg) {
		if (msg instanceof APIMessage) {
		} else {
		}
	}

	@Override
	public String getId() {
		return null;
	}
}
