package org.zstack.storage.ceph.backup

import org.zstack.header.errorcode.ErrorCode

doc {

	title "为Ceph镜像服务器添加 Mon节点"

	ref {
		name "error"
		path "org.zstack.storage.ceph.backup.APIAddMonToCephBackupStorageEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.storage.ceph.backup.APIAddMonToCephBackupStorageEvent.inventory"
		desc "null"
		type "CephBackupStorageInventory"
		since "0.6"
		clz CephBackupStorageInventory.class
	}
}
