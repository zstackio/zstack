package org.zstack.header.host

import org.zstack.header.host.APIGetPhysicalMachineBlockDevicesReply.BlockDevices.BlockDevice

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
		path "org.zstack.header.host.APIGetPhysicalMachineBlockDevicesReply.BlockDevices.BlockDevice.children"
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
}
