package org.zstack.header.volumeCache



doc {

	title "卷缓存模式"

	field {
		name "WriteBack"
		desc "写回模式，写入数据先进入缓存再异步回写到后端卷"
		type "VolumeCacheMode"
		since "5.5.28"
	}
}
