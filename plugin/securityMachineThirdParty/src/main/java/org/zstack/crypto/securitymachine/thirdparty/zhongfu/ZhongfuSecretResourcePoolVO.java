package org.zstack.crypto.securitymachine.thirdparty.zhongfu;


import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;

import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name = "uuid", referencedColumnName = "uuid")
public class ZhongfuSecretResourcePoolVO extends SecretResourcePoolVO {
	public ZhongfuSecretResourcePoolVO() {}

	public ZhongfuSecretResourcePoolVO(SecretResourcePoolVO vo) {
		super(vo);
	}
}
