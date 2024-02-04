package org.zstack.crypto.securitymachine.thirdparty.westone;


import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(WestoneSecretResourcePoolVO.class)
public class WestoneSecretResourcePoolVO_ extends SecretResourcePoolVO_ {
	public static volatile SingularAttribute<WestoneSecretResourcePoolVO, String> tenantId;

	public static volatile SingularAttribute<WestoneSecretResourcePoolVO, String> appId;

	public static volatile SingularAttribute<WestoneSecretResourcePoolVO, String> secret;

	public static volatile SingularAttribute<WestoneSecretResourcePoolVO, String> initParamUrl;

	public static volatile SingularAttribute<WestoneSecretResourcePoolVO, String> initParamWorkdId;
}
