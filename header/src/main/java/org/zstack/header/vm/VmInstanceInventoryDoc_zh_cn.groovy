package org.zstack.header.vm

import java.lang.Long
import java.lang.Integer
import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.header.vm.VmNicInventory
import org.zstack.header.volume.VolumeInventory
import org.zstack.header.vm.cdrom.VmCdRomInventory

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "0.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "0.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "0.6"
	}
	field {
		name "zoneUuid"
		desc "区域UUID"
		type "String"
		since "0.6"
	}
	field {
		name "clusterUuid"
		desc "集群UUID"
		type "String"
		since "0.6"
	}
	field {
		name "imageUuid"
		desc "镜像UUID"
		type "String"
		since "0.6"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "0.6"
	}
	field {
		name "lastHostUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "instanceOfferingUuid"
		desc "计算规格UUID"
		type "String"
		since "0.6"
	}
	field {
		name "rootVolumeUuid"
		desc "根云盘UUID"
		type "String"
		since "0.6"
	}
	field {
		name "platform"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "architecture"
		desc "架构类型"
		type "String"
		since "4.1.1"
	}
	field {
		name "defaultL3NetworkUuid"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "type"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "hypervisorType"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "memorySize"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "cpuNum"
		desc ""
		type "Integer"
		since "0.6"
	}
	field {
		name "cpuSpeed"
		desc ""
		type "Long"
		since "0.6"
	}
	field {
		name "allocatorStrategy"
		desc ""
		type "String"
		since "0.6"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "0.6"
	}
	field {
		name "state"
		desc ""
		type "String"
		since "0.6"
	}
	ref {
		name "vmNics"
		path "org.zstack.header.vm.VmInstanceInventory.vmNics"
		desc "null"
		type "List"
		since "0.6"
		clz VmNicInventory.class
	}
	ref {
		name "allVolumes"
		path "org.zstack.header.vm.VmInstanceInventory.allVolumes"
		desc "null"
		type "List"
		since "0.6"
		clz VolumeInventory.class
	}
	ref {
		name "vmCdRoms"
		path "org.zstack.header.vm.VmInstanceInventory.vmCdRoms"
		desc ""
		type "List"
		since ""
		clz VmCdRomInventory.class
	}
	field {
		name "guestOsType"
		desc "操作系统类型"
		type "String"
		since "4.1.2"
	}
}
