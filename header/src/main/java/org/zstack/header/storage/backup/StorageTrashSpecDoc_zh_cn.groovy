package org.zstack.header.storage.backup

doc {

	title "回收数据清单"

	field {
		name "resourceUuid"
		desc "回收数据对应的原资源UUID"
		type "String"
		since "3.2.0"
	}
	field {
		name "resourceType"
		desc "回收数据对应的资源类型"
		type "String"
		since "3.2.0"
	}
	field {
		name "storageUuid"
		desc "回收数据所在的存储UUID"
		type "String"
		since "3.2.0"
	}
	field {
		name "storageType"
		desc "回收数据所在的存储类型"
		type "String"
		since "3.2.0"
	}
	field {
		name "installPath"
		desc "回收数据的地址"
		type "String"
		since "3.2.0"
	}
	field {
		name "isFolder"
		desc "是否目录"
		type "boolean"
		since "3.2.0"
	}
	field {
		name "hypervisorType"
		desc "回收数据的虚拟机管理程序类型"
		type "String"
		since "3.2.0"
	}
	field {
		name "size"
		desc "回收数据的大小"
		type "Long"
		since "3.2.0"
	}
}
