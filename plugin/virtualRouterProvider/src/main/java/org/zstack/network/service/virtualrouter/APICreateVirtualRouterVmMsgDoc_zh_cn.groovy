package org.zstack.network.service.virtualrouter

import org.zstack.header.vm.APICreateVmInstanceEvent

doc {
    title "创建虚拟路由器(CreateVirtualRouterVm)"

    category "虚拟路由器"

    desc "创建虚拟路由器"

    rest {
        request {
			url "POST /v1/vm-instances/appliances/virtual-routers"


            header (OAuth: 'the-session-uuid')

            clz APICreateVirtualRouterVmMsg.class

            desc ""
            
			params {

				column {
					name "managementNetworkUuid"
					enclosedIn "params"
					desc "管理L3网络UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "publicNetworkUuid"
					enclosedIn "params"
					desc "公有L3网络UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "networkServicesProvided"
					enclosedIn "params"
					desc "网络服务模块"
					location "body"
					type "Set"
					optional false
					since "0.6"
					
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "instanceOfferingUuid"
					enclosedIn "params"
					desc "计算规格UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "imageUuid"
					enclosedIn "params"
					desc "镜像UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "l3NetworkUuids"
					enclosedIn "params"
					desc "三层网络UUID"
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "type"
					enclosedIn "params"
					desc "类型"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("UserVm","ApplianceVm")
				}
				column {
					name "rootDiskOfferingUuid"
					enclosedIn "params"
					desc "根磁盘规格UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "dataDiskOfferingUuids"
					enclosedIn "params"
					desc "数据盘规格UUID"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "clusterUuid"
					enclosedIn "params"
					desc "集群UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "hostUuid"
					enclosedIn "params"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "primaryStorageUuidForRootVolume"
					enclosedIn "params"
					desc "根磁盘主存储UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "defaultL3NetworkUuid"
					enclosedIn "params"
					desc "默认三层网络UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "strategy"
					enclosedIn "params"
					desc "策略"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("InstantStart","JustCreate")
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APICreateVmInstanceEvent.class
        }
    }
}