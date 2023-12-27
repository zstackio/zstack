package org.zstack.crypto.securitymachine.thirdparty.westone;


import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
public class WestoneSecretResourcePoolVO extends SecretResourcePoolVO {
	@Column
	private String tenantId;
	@Column
	private String appId;
	@Column
	private String secret;
	@Column
	private String initParamUrl;
	@Column
	private String initParamWorkdId;
	@Column
	private String initParamWorkdir;

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

	public String getInitParamWorkdir() {
		return initParamWorkdir;
	}

	public void setInitParamWorkdir(String initParamWorkdir) {
		this.initParamWorkdir = initParamWorkdir;
	}

	public WestoneSecretResourcePoolVO() {}

	public WestoneSecretResourcePoolVO(SecretResourcePoolVO vo) {
		super(vo);
	}
}
