package org.zstack.crypto.securitymachine.thirdparty.zhongfu;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ZhongfuSecretResourcePoolVO.class, collectionValueOfMethod = "valueOf1", parent = {@Parent(inventoryClass = SecretResourcePoolInventory.class, type = "Zhongfu")})
public class ZhongfuSecretResourcePoolInventory extends SecretResourcePoolInventory {

	protected ZhongfuSecretResourcePoolInventory(ZhongfuSecretResourcePoolVO vo) {
		super(vo);
	}

	public ZhongfuSecretResourcePoolInventory() {}

	public static ZhongfuSecretResourcePoolInventory valueOf(ZhongfuSecretResourcePoolVO vo) {
		return new ZhongfuSecretResourcePoolInventory(vo);
	}

	public static List<ZhongfuSecretResourcePoolInventory> valueOf1(Collection<ZhongfuSecretResourcePoolVO> vos) {
		List<ZhongfuSecretResourcePoolInventory> invs = new ArrayList<ZhongfuSecretResourcePoolInventory>();
		for (ZhongfuSecretResourcePoolVO vo : vos) {
			invs.add(valueOf(vo));
		}
		return invs;
	}
}
