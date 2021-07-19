package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.network.l3.L3NetworkInventory

doc {

	title "云主机网卡可加载网络清单"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetCandidateL3NetworksForChangeVmNicNetworkReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "4.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.vm.APIGetCandidateL3NetworksForChangeVmNicNetworkReply.inventories"
		desc "null"
		type "List"
		since "4.1.0"
		clz L3NetworkInventory.class
	}
}
