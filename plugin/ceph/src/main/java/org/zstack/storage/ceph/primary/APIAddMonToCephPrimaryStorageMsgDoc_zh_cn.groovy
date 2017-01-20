package org.zstack.storage.ceph.primary

doc {
    title "为 Ceph 主存储添加 mon 节点(AddMonToCephPrimaryStorage)"

    category "stroage.ceph.primary"

    desc "为 Ceph 主存储添加 mon 节点"

   rest {
        request {
			url "POST /v1/primary-storage/ceph/{uuid}/mons"


            header (OAuth: 'the-session-uuid')

            clz APIAddMonToCephPrimaryStorageMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "Ceph 主存储的UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "monUrls"
					enclosedIn "params"
					desc "mon 节点地址列表"
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn "params"
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn "params"
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIAddMonToCephPrimaryStorageEvent.class
        }
    }
}