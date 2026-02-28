package org.zstack.header.localVolumeCache

import org.zstack.header.localVolumeCache.VmLocalVolumeCachePoolState
import org.zstack.header.localVolumeCache.VmLocalVolumeCachePoolStatus
import org.zstack.header.localVolumeCache.VmLocalVolumeCacheInventory
import java.sql.Timestamp

doc {

	title "在这里输入结构的名称"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.5.6"
	}
	field {
		name "hostUuid"
		desc "物理机UUID"
		type "String"
		since "5.5.6"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "5.5.6"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "5.5.6"
	}
	field {
		name "metadata"
		desc ""
		type "LinkedHashMap"
		since "5.5.6"
	}
	field {
		name "totalCapacity"
		desc ""
		type "long"
		since "5.5.6"
	}
	field {
		name "availableCapacity"
		desc ""
		type "long"
		since "5.5.6"
	}
	ref {
		name "state"
		path "org.zstack.header.localVolumeCache.VmLocalVolumeCachePoolInventory.state"
		desc "null"
		type "VmLocalVolumeCachePoolState"
		since "5.5.6"
		clz VmLocalVolumeCachePoolState.class
	}
	ref {
		name "status"
		path "org.zstack.header.localVolumeCache.VmLocalVolumeCachePoolInventory.status"
		desc "null"
		type "VmLocalVolumeCachePoolStatus"
		since "5.5.6"
		clz VmLocalVolumeCachePoolStatus.class
	}
	ref {
		name "caches"
		path "org.zstack.header.localVolumeCache.VmLocalVolumeCachePoolInventory.caches"
		desc "null"
		type "Set"
		since "5.5.6"
		clz VmLocalVolumeCacheInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "5.5.6"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "5.5.6"
	}
}
