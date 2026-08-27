package org.zstack.physicalserver

import java.lang.Long

doc {

	title "物理服务器管控服务资源使用清单"

	field {
		name "roleType"
		desc "服务所属的Role类型"
		type "String"
		since "5.5.38"
	}
	field {
		name "serviceName"
		desc "服务在Role清单或Provider中登记的稳定名称"
		type "String"
		since "5.5.38"
	}
	field {
		name "restartable"
		desc "是否允许Cloud通过托管服务重启API重启该服务"
		type "boolean"
		since "5.5.38"
	}
	field {
		name "restartRequired"
		desc "服务是否需要重启后才能进入目标Role资源边界"
		type "boolean"
		since "5.5.38"
	}
	field {
		name "state"
		desc "服务当前观测状态，例如RUNNING、INACTIVE、NOT_FOUND或UNAVAILABLE"
		type "String"
		since "5.5.38"
	}
	field {
		name "cpuSet"
		desc "服务当前允许运行的逻辑CPU集合"
		type "String"
		since "5.5.38"
	}
	field {
		name "cpuTime"
		desc "服务所在cgroup的累计CPU时间，单位为纳秒"
		type "Long"
		since "5.5.38"
	}
	field {
		name "memory"
		desc "服务当前使用的内存，单位为字节"
		type "Long"
		since "5.5.38"
	}
	field {
		name "memoryLimit"
		desc "服务包含父Role约束后的有效内存上限，单位为字节，0表示不限制"
		type "Long"
		since "5.5.38"
	}
}
