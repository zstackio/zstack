package org.zstack.header.vm

import org.zstack.header.storage.primary.PrimaryStorageInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "创建云主机可选主存储的结果"

	ref {
		name "rootVolumePrimaryStorages"
		path "org.zstack.header.vm.APIGetCandidatePrimaryStoragesForCreatingVmReply.rootVolumePrimaryStorages"
		desc "根云盘可选主存储"
		type "List"
		since "2.1"
		clz PrimaryStorageInventory.class
	}
	field {
		name "dataVolumePrimaryStorages"
		desc "数据云盘可选主存储，分别对应每一个数据云盘规格"
		type "Map"
		since "2.1"
	}
	field {
		name "success"
		desc "操作是否成功"
		type "boolean"
		since "2.1"
	}
	ref {
		name "error"
		path "org.zstack.header.vm.APIGetCandidatePrimaryStoragesForCreatingVmReply.error"
		desc "如果不为null，代表操作失败，记录了操作失败的原因"
		type "ErrorCode"
		since "2.1"
		clz ErrorCode.class
	}
}
