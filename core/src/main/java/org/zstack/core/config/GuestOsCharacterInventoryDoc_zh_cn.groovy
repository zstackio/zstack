package org.zstack.core.config

import java.lang.Boolean

doc {

	title "虚拟机操作系统清单"

	field {
		name "architecture"
		desc "系统架构x86_64, aarch64等"
		type "String"
		since "5.1.0"
	}
	field {
		name "platform"
		desc "平台CentOS, Ubuntu等"
		type "String"
		since "5.1.0"
	}
	field {
		name "osRelease"
		desc "操作系统版本7, 8等"
		type "String"
		since "5.1.0"
	}
	field {
		name "acpi"
		desc "是否启用acpi"
		type "Boolean"
		since "5.1.0"
	}
	field {
		name "hygonTag"
		desc "是否运行在海光处理器上"
		type "Boolean"
		since "5.1.0"
	}
	field {
		name "x2apic"
		desc "是否启用x2apic"
		type "Boolean"
		since "5.1.0"
	}
	field {
		name "cpuModel"
		desc "CPU型号"
		type "String"
		since "5.1.0"
	}
	field {
		name "nicDriver"
		desc "网卡驱动"
		type "String"
		since "5.1.0"
	}
}
