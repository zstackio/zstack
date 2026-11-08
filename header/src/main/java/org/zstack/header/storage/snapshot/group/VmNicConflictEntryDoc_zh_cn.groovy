package org.zstack.header.storage.snapshot.group



doc {

	title "网卡冲突条目"

	field {
		name "ip"
		desc "与快照组网卡存档重复的 IP 地址；仅 MAC 冲突时为空。"
		type "String"
		since "5.5.38"
	}
	field {
		name "mac"
		desc "与快照组网卡存档重复的 MAC 地址；仅 IP 冲突时为空。"
		type "String"
		since "5.5.38"
	}
	field {
		name "vmNicName"
		desc "冲突网卡的内部名称；无权查看其云主机时为空。"
		type "String"
		since "5.5.38"
	}
	field {
		name "vmInstanceName"
		desc "冲突网卡所属云主机的名称；无权查看该云主机时为空。"
		type "String"
		since "5.5.38"
	}
	field {
		name "vmInstanceUuid"
		desc "冲突网卡所属云主机的 UUID；无权查看该云主机时为空。"
		type "String"
		since "5.5.38"
	}
}
