package org.zstack.header.volumeCache

import org.zstack.header.volumeCache.VolumeCacheMode
import org.zstack.header.volumeCache.VolumeCacheStatus
import java.lang.Long
import java.sql.Timestamp

doc {

	title "卷缓存清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "5.5.28"
	}
	field {
		name "volumeUuid"
		desc "云盘UUID"
		type "String"
		since "5.5.28"
	}
	field {
		name "poolUuid"
		desc "主机缓存存储的UUID"
		type "String"
		since "5.5.28"
	}
	field {
		name "installPath"
		desc "卷缓存在主机上的安装路径"
		type "String"
		since "5.5.28"
	}
	ref {
		name "cacheMode"
		path "org.zstack.header.volumeCache.VolumeCacheInventory.cacheMode"
		desc "卷缓存的读写模式"
		type "VolumeCacheMode"
		since "5.5.28"
		clz VolumeCacheMode.class
	}
	ref {
		name "status"
		path "org.zstack.header.volumeCache.VolumeCacheInventory.status"
		desc "卷缓存的状态"
		type "VolumeCacheStatus"
		since "5.5.28"
		clz VolumeCacheStatus.class
	}
	field {
		name "virtualSize"
		desc "卷缓存虚拟大小"
		type "Long"
		since "5.5.28"
	}
	field {
		name "actualSize"
		desc "卷缓存实际占用大小"
		type "Long"
		since "5.5.28"
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
