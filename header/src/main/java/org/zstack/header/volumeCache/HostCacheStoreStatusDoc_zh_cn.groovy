package org.zstack.header.volumeCache



doc {

	title "主机缓存存储连接状态"

	field {
		name "Connecting"
		desc "正在与主机缓存存储建立连接"
		type "HostCacheStoreStatus"
		since "5.5.28"
	}
	field {
		name "Connected"
		desc "已连接，可正常执行缓存存储操作"
		type "HostCacheStoreStatus"
		since "5.5.28"
	}
	field {
		name "Disconnected"
		desc "连接断开，当前不可执行缓存存储操作"
		type "HostCacheStoreStatus"
		since "5.5.28"
	}
}
