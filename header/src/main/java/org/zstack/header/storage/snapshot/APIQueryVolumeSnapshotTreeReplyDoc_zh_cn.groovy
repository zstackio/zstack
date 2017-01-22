package org.zstack.header.storage.snapshot

import org.zstack.header.errorcode.ErrorCode

doc {

	title "快照树清单"

	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotTreeReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotTreeReply.inventories"
		desc "null"
		type "List"
		since "0.6"
		clz VolumeSnapshotTreeInventory.class
	}
}
