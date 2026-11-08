package org.zstack.header.storage.snapshot.group

import org.zstack.header.storage.snapshot.group.VmNicConflictEntry
import org.zstack.header.errorcode.ErrorCode

doc {

	title "内存快照组网卡冲突检测结果"

	ref {
		name "vmNicConflict"
		path "org.zstack.header.storage.snapshot.group.APICheckMemorySnapshotGroupConflictReply.vmNicConflict"
		desc "与快照组网卡存档存在 IP 或 MAC 重复的网卡列表；没有检测到冲突时为空列表。冲突云主机不可见时隐藏其身份及网卡内部名称。"
		type "List"
		since "5.5.38"
		clz VmNicConflictEntry.class
	}
	field {
		name "success"
		desc "本次检测请求是否成功；true 不表示未发现冲突。"
		type "boolean"
		since "5.5.38"
	}
	ref {
		name "error"
		path "org.zstack.header.storage.snapshot.group.APICheckMemorySnapshotGroupConflictReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "5.5.38"
		clz ErrorCode.class
	}
}
