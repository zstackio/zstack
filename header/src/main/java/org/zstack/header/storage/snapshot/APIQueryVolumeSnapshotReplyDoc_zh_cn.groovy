package org.zstack.header.storage.snapshot

import org.zstack.header.errorcode.ErrorCode

doc {

	title "云盘快照清单"

	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz VolumeSnapshotInventory.class
	}
}
