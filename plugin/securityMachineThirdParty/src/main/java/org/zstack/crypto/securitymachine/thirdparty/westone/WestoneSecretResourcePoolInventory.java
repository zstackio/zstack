package org.zstack.crypto.securitymachine.thirdparty.westone;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = WestoneSecretResourcePoolVO.class, collectionValueOfMethod = "valueOf1", parent = {@Parent(inventoryClass = SecretResourcePoolInventory.class, type = "WestoneSec")})
public class WestoneSecretResourcePoolInventory extends SecretResourcePoolInventory {
	private String tenantId;
	private String appId;
	private String secret;
	private String initParamUrl;
	private String initParamWorkdId;

	protected WestoneSecretResourcePoolInventory(WestoneSecretResourcePoolVO vo) {
		super(vo);
		this.tenantId=vo.getTenantId();
		this.appId=vo.getAppId();
		this.secret=vo.getSecret();
		this.initParamUrl=vo.getInitParamUrl();
		this.initParamWorkdId=vo.getInitParamWorkdId();
	}

	public WestoneSecretResourcePoolInventory() {}

	public static WestoneSecretResourcePoolInventory valueOf(WestoneSecretResourcePoolVO vo) {
		return new WestoneSecretResourcePoolInventory(vo);
	}

	public static List<WestoneSecretResourcePoolInventory> valueOf1(Collection<WestoneSecretResourcePoolVO> vos) {
		List<WestoneSecretResourcePoolInventory> invs = new ArrayList<WestoneSecretResourcePoolInventory>();
		for (WestoneSecretResourcePoolVO vo : vos) {
			invs.add(valueOf(vo));
		}
		return invs;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getInitParamUrl() {
		return initParamUrl;
	}

	public void setInitParamUrl(String initParamUrl) {
		this.initParamUrl = initParamUrl;
	}

	public String getInitParamWorkdId() {
		return initParamWorkdId;
	}

	public void setInitParamWorkdId(String initParamWorkdId) {
		this.initParamWorkdId = initParamWorkdId;
	}
}
