package org.zstack.header.image

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotEvent.Failure

doc {

	title "镜像清单"

	ref {
		name "error"
		path "org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "0.6"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotEvent.inventory"
		desc "null"
		type "ImageInventory"
		since "0.6"
		clz ImageInventory.class
	}
	ref {
		name "failures"
		path "org.zstack.header.image.APICreateRootVolumeTemplateFromVolumeSnapshotEvent.failuresOnBackupStorage"
		desc "null"
		type "List"
		since "0.6"
		clz Failure.class
	}
}
