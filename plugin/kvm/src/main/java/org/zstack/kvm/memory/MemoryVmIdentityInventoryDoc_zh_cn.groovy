package org.zstack.kvm.memory

doc {

	title "VM记账身份"

	field { name "uuid"; desc "云主机UUID。"; type "String"; since "5.5.38" }
	field { name "hostBootId"; desc "Host启动标识。"; type "String"; since "5.5.38" }
	field { name "instanceGeneration"; desc "实例代次不透明标识；按原始字符串返回。"; type "String"; since "5.5.38" }
	field { name "mmContextId"; desc "内存管理上下文标识，以字符串保留无符号整数精度。"; type "String"; since "5.5.38" }
	field { name "cgroupId"; desc "cgroup标识，以字符串保留无符号整数精度。"; type "String"; since "5.5.38" }
	field { name "startTime"; desc "进程/proc启动tick，无符号十进制字符串；不是墙钟日期时间。"; type "String"; since "5.5.38" }
	field { name "sampledMonotonicNs"; desc "单调时钟采样时间，以字符串保留精度。"; type "String"; since "5.5.38" }
	field { name "cgroupInode"; desc "cgroup inode，以字符串保留无符号整数精度。"; type "String"; since "5.5.38" }
	field { name "sequence"; desc "身份样本序列号，以字符串保留精度。"; type "String"; since "5.5.38" }
	field { name "cgroupPath"; desc "VM对应的cgroup路径。"; type "String"; since "5.5.38" }
	field { name "poolGeneration"; desc "ZRAM池代次，无符号64位整数的十进制字符串。"; type "String"; since "5.5.38" }
	field { name "device"; desc "ZRAM设备标识。"; type "String"; since "5.5.38" }
	field { name "pid"; desc "VM QEMU进程号。"; type "Integer"; since "5.5.38" }
	field { name "instance_generation"; desc "已弃用的r12 VM列表兼容别名；优先使用instanceGeneration。值为不透明字符串，无数值单位；未观测时为空，与主字段保持一致。UI迁移及兼容期结束后移除。"; type "String"; since "5.5.38" }
}
