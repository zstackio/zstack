package org.zstack.header.host



doc {

	title "网路服务类型"

	field {
		name "ManagementNetwork"
		desc "管理网络"
		type "HostNetworkInterfaceServiceType"
		since "4.7.11"
	}
	field {
		name "TenantNetwork"
		desc "业务网络"
		type "HostNetworkInterfaceServiceType"
		since "4.7.11"
	}
	field {
		name "StorageNetwork"
		desc "存储网络"
		type "HostNetworkInterfaceServiceType"
		since "4.7.11"
	}
	field {
		name "BackupNetwork"
		desc "备份网络"
		type "HostNetworkInterfaceServiceType"
		since "4.7.11"
	}
	field {
		name "MigrationNetwork"
		desc "迁移网络"
		type "HostNetworkInterfaceServiceType"
		since "4.7.11"
	}
}
