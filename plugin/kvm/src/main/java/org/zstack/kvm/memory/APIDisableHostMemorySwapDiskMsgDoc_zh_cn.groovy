package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIDisableHostMemorySwapDiskEvent

doc {
    title "DisableHostMemorySwapDisk"

    category "memoryOptimization"

    desc """停用物理机交换盘（以长任务提交）：先把交换出去的数据读回内存（swapoff），再删除开机挂载和交换签名，盘保留。内存装不下交换出去的数据时拒绝，并给出差额。"""

    rest {
        request {
			url "PUT /v1/hosts/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDisableHostMemorySwapDiskMsg.class

            desc """停用物理机交换盘"""
            
			params {

				column {
					name "uuid"
					enclosedIn "disableHostMemorySwapDisk"
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "5.7.10"
				}
				column {
					name "resourceUuid"
					enclosedIn "disableHostMemorySwapDisk"
					desc "长任务的UUID，可选"
					location "body"
					type "String"
					optional true
					since "5.7.10"
				}
				column {
					name "tagUuids"
					enclosedIn "disableHostMemorySwapDisk"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "5.7.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "5.7.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "5.7.10"
				}
			}
        }

        response {
            clz APIDisableHostMemorySwapDiskEvent.class
        }
    }
}