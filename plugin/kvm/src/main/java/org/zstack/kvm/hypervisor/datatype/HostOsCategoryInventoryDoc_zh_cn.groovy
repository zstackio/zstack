package org.zstack.kvm.hypervisor.datatype

import org.zstack.kvm.hypervisor.datatype.KvmHostHypervisorMetadataInventory
import java.sql.Timestamp

doc {

	title "支持的物理机类型条目"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该条目"
		type "String"
		since "4.6.21"
	}
	field {
		name "architecture"
		desc "物理机架构，比如 \"x86_64\""
		type "String"
		since "4.6.21"
	}
	field {
		name "osReleaseVersion"
		desc "物理机操作系统版本，比如 \"centos Core 7.6.1810\""
		type "String"
		since "4.6.21"
	}
	ref {
		name "metadataList"
		path "org.zstack.kvm.hypervisor.datatype.HostOsCategoryInventory.metadataList"
		desc "null"
		type "List"
		since "4.6.21"
		clz KvmHostHypervisorMetadataInventory.class
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "4.6.21"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "4.6.21"
	}
}
