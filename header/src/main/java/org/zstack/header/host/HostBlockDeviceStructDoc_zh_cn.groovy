package org.zstack.header.host

import java.lang.Long

doc {

	title "主机块设备结构"

	field {
		name "name"
		desc "块设备名称"
		type "String"
		since "5.5.28"
	}
	field {
		name "wwid"
		desc "块设备全局唯一标识"
		type "String"
		since "5.5.28"
	}
	field {
		name "vendor"
		desc "块设备厂商"
		type "String"
		since "5.5.28"
	}
	field {
		name "model"
		desc "块设备型号"
		type "String"
		since "5.5.28"
	}
	field {
		name "wwn"
		desc "块设备全局名称"
		type "String"
		since "5.5.28"
	}
	field {
		name "serial"
		desc "块设备序列号"
		type "String"
		since "5.5.28"
	}
	field {
		name "hctl"
		desc "块设备的 HCTL 地址"
		type "String"
		since "5.5.28"
	}
	field {
		name "type"
		desc "块设备类型"
		type "String"
		since "5.5.28"
	}
	field {
		name "path"
		desc "块设备路径"
		type "String"
		since "5.5.28"
	}
	field {
		name "size"
		desc "块设备容量，单位为字节"
		type "Long"
		since "5.5.28"
	}
	field {
		name "source"
		desc "块设备来源"
		type "String"
		since "5.5.28"
	}
	field {
		name "transport"
		desc "块设备传输协议"
		type "String"
		since "5.5.28"
	}
	field {
		name "targetIdentifier"
		desc "块设备目标标识"
		type "String"
		since "5.5.28"
	}
}
