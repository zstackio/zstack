package org.zstack.header.storage.snapshot.group

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory

doc {

	title "查询快照组结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.group.APIQueryVolumeSnapshotGroupReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.6.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.header.storage.snapshot.group.APIQueryVolumeSnapshotGroupReply.inventories"
		desc "快照组清单"
		type "List"
		since "3.6.0"
		clz VolumeSnapshotGroupInventory.class
	}
}
