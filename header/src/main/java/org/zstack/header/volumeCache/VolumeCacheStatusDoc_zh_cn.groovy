package org.zstack.header.volumeCache



doc {

	title "卷缓存状态"

	field {
		name "NotInstantiated"
		desc "卷缓存尚未在主机缓存存储上实例化"
		type "VolumeCacheStatus"
		since "5.5.28"
	}
	field {
		name "Ready"
		desc "卷缓存已就绪，可供虚拟机使用"
		type "VolumeCacheStatus"
		since "5.5.28"
	}
}
