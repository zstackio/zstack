package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIGetHostMemorySwapDisksReply

doc {
    title "GetHostMemorySwapDisks"

    category "memoryOptimization"

    desc """查询物理机的交换盘及可作为交换盘的本地NVMe盘或分区，以及当前能否准备。"""

    rest {
        request {
			url "GET /v1/hosts/{hostUuid}/memory-swap-disks"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIGetHostMemorySwapDisksMsg.class

            desc """容量单位为字节。只读，用于选择交换盘；准备时物理机会再次检查。"""
            
			params {

				column {
					name "hostUuid"
					enclosedIn ""
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "5.7.10"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "5.7.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "5.7.10"
				}
			}
        }

        response {
            clz APIGetHostMemorySwapDisksReply.class
        }
    }
}