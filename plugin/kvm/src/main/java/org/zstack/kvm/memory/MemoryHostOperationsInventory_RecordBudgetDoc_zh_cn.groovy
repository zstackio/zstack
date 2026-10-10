package org.zstack.kvm.memory

doc {

	title "Host内存操作记录容量"

	field { name "actualAllocatedBytes"; desc "记录实际占用的字节数。"; type "Long"; since "5.5.38" }
	field { name "reservedBytes"; desc "记录预留的字节数。"; type "Long"; since "5.5.38" }
	field { name "budgetBytes"; desc "记录容量额度，单位字节。"; type "Long"; since "5.5.38" }
	field { name "records"; desc "当前记录总数。"; type "Integer"; since "5.5.38" }
	field { name "protectedRecords"; desc "因未决或恢复保护而不能清理的记录数。"; type "Integer"; since "5.5.38" }
}
