package org.zstack.header.localVolumeCache

import org.zstack.header.localVolumeCache.VmLocalVolumeCacheMode
import org.zstack.header.localVolumeCache.VmLocalVolumeCacheState
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
		name "volumeUuid"
		desc "云盘UUID"
		type "String"
		since "5.5.6"
	}
	field {
		name "poolUuid"
		desc ""
		type "String"
		since "5.5.6"
	}
	field {
		name "installPath"
		desc ""
		type "String"
		since "5.5.6"
	}
	ref {
		name "cacheMode"
		path "org.zstack.header.localVolumeCache.VmLocalVolumeCacheInventory.cacheMode"
		desc "null"
		type "VmLocalVolumeCacheMode"
		since "5.5.6"
		clz VmLocalVolumeCacheMode.class
	}
	ref {
		name "state"
		path "org.zstack.header.localVolumeCache.VmLocalVolumeCacheInventory.state"
		desc "null"
		type "VmLocalVolumeCacheState"
		since "5.5.6"
		clz VmLocalVolumeCacheState.class
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
