package org.zstack.header.vm

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.volume.VolumeInventory

doc {

    title "可加载云盘"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
    ref {
        name "error"
        path "org.zstack.header.vm.APIGetVmAttachableDataVolumeReply.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
    ref {
        name "inventories"
        path "org.zstack.header.vm.APIGetVmAttachableDataVolumeReply.inventories"
        desc "null"
        type "List"
        since "0.6"
        clz VolumeInventory.class
    }
}
