package org.zstack.header.host

import org.zstack.header.host.BlockDevices.BlockDevice

doc {

	title "物理机单个磁盘信息"

	field {
		name "name"
		desc "硬盘设备名字"
		type "String"
		since "zsv 4.3.0"
	}
	field {
		name "type"
		desc "磁盘类型"
		type "String"
		since "zsv 4.3.0"
	}
	field {
		name "size"
		desc "磁盘容量"
		type "long"
		since "zsv 4.3.0"
	}
	field {
		name "used"
		desc "磁盘已使用容量"
		type "long"
		since "zsv 4.10.0"
	}
	field {
		name "available"
		desc "磁盘可使用容量"
		type "long"
		since "zsv 4.10.0"
	}
	field {
		name "physicalSector"
		desc "物理扇区大小"
		type "long"
		since "zsv 4.3.0"
	}
	field {
		name "logicalSector"
		desc "逻辑扇区大小"
		type "long"
		since "zsv 4.3.0"
	}
	field {
		name "mountPoint"
		desc "挂载点"
		type "String"
		since "zsv 4.3.0"
	}
	ref {
		name "children"
		path "org.zstack.header.host.BlockDevices.BlockDevice.children"
		desc "子分区"
		type "List"
		since "zsv 4.3.0"
		clz BlockDevice.class
	}
	field {
		name "partitionTable"
		desc "分区表类型"
		type "String"
		since "zsv 4.3.0"
	}
	field {
		name "FSType"
		desc "文件系统类型"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "serialNumber"
		desc "序列号"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "model"
		desc "型号"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "mediaType"
		desc "介质类型"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "usedRatio"
		desc "使用率"
		type "String"
		since "zsv 4.10.0"
	}
	field {
		name "status"
		desc "磁盘状态"
		type "Boolean"
		since "zsv 4.10.0"
	}
}
