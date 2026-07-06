package org.zstack.header.volumeCache

import org.zstack.header.volumeCache.HostCacheStoreState
import org.zstack.header.volumeCache.HostCacheStoreStatus
import org.zstack.header.volumeCache.HostCacheStoreDeviceInventory
import org.zstack.header.volumeCache.VolumeCacheInventory
import java.sql.Timestamp

doc {

	title "主机缓存存储清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.5.28"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "5.5.28"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.5.28"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.5.28"
	}
	field {
		name "mountPoint"
		desc "缓存池挂载路径"
		type "String"
		since "5.5.28"
	}
	field {
		name "totalCapacity"
		desc "主机缓存存储的总容量，单位为字节"
		type "long"
		since "5.5.28"
	}
	field {
		name "availableCapacity"
		desc "主机缓存存储的可用容量，单位为字节"
		type "long"
		since "5.5.28"
	}
	field {
		name "totalPhysicalCapacity"
		desc "主机缓存存储的物理总容量，单位为字节"
		type "long"
		since "5.5.28"
	}
	field {
		name "availablePhysicalCapacity"
		desc "主机缓存存储的物理可用容量，单位为字节"
		type "long"
		since "5.5.28"
	}
	field {
		name "systemUsedCapacity"
		desc "主机缓存存储中非缓存文件占用的容量，单位为字节"
		type "long"
		since "5.5.28"
	}
	ref {
		name "state"
		path "org.zstack.header.volumeCache.HostCacheStoreInventory.state"
		desc "主机缓存存储的状态"
		type "HostCacheStoreState"
		since "5.5.28"
		clz HostCacheStoreState.class
	}
	ref {
		name "status"
		path "org.zstack.header.volumeCache.HostCacheStoreInventory.status"
		desc "主机缓存存储的健康状态"
		type "HostCacheStoreStatus"
		since "5.5.28"
		clz HostCacheStoreStatus.class
	}
	ref {
		name "devices"
		path "org.zstack.header.volumeCache.HostCacheStoreInventory.devices"
		desc "组成该主机缓存存储的块设备列表"
		type "List"
		since "5.5.28"
		clz HostCacheStoreDeviceInventory.class
	}
	ref {
		name "caches"
		path "org.zstack.header.volumeCache.HostCacheStoreInventory.caches"
		desc "该主机缓存存储上的卷缓存集合"
		type "Set"
		since "5.5.28"
		clz VolumeCacheInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.5.28"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.5.28"
	}
}
