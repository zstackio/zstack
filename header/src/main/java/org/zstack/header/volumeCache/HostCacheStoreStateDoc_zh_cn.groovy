package org.zstack.header.volumeCache



doc {

	title "主机缓存存储启用状态"

	field {
		name "Enabled"
		desc "主机缓存存储已启用，可参与缓存相关操作"
		type "HostCacheStoreState"
		since "5.5.28"
	}
	field {
		name "Disabled"
		desc "主机缓存存储已禁用，不参与缓存相关操作"
		type "HostCacheStoreState"
		since "5.5.28"
	}
}
