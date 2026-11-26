package org.zstack.kvm.memory

import org.zstack.kvm.memory.APIPrepareHostMemorySwapDiskEvent

doc {
    title "PrepareHostMemorySwapDisk"

    category "memoryOptimization"

    desc """把物理机上的一块本地NVMe盘或分区准备为交换盘并启用（以长任务提交）：mkswap、开启discard的swapon，并按交换签名UUID写入开机挂载，物理机重启后自动启用。candidateId取自GetHostMemorySwapDisks；内存优化服务在运行或已有其他交换设备时拒绝。盘上已有数据时须overwrite=true，调用前应由界面二次确认。"""

    rest {
        request {
			url "PUT /v1/hosts/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIPrepareHostMemorySwapDiskMsg.class

            desc """准备并启用物理机交换盘"""
            
			params {

				column {
					name "uuid"
					enclosedIn "prepareHostMemorySwapDisk"
					desc "物理机UUID"
					location "url"
					type "String"
					optional false
					since "5.7.10"
				}
				column {
					name "candidateId"
					enclosedIn "prepareHostMemorySwapDisk"
					desc "GetHostMemorySwapDisks返回的candidates[].id"
					location "body"
					type "String"
					optional false
					since "5.7.10"
				}
				column {
					name "overwrite"
					enclosedIn "prepareHostMemorySwapDisk"
					desc "盘上已有数据时确认覆盖；候选状态为NeedsOverwriteConfirmation时必须为true"
					location "body"
					type "Boolean"
					optional true
					since "5.7.10"
				}
				column {
					name "resourceUuid"
					enclosedIn "prepareHostMemorySwapDisk"
					desc "长任务的UUID，可选"
					location "body"
					type "String"
					optional true
					since "5.7.10"
				}
				column {
					name "tagUuids"
					enclosedIn "prepareHostMemorySwapDisk"
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
            clz APIPrepareHostMemorySwapDiskEvent.class
        }
    }
}