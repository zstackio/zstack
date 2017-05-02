package org.zstack.header.storage.primary

doc {
    title "更改主存储状态(ChangePrimaryStorageState)"

    category "storage.primary"

    desc "更改主存储状态"

    rest {
        request {
			url "PUT /v1/primary-storage/{uuid}/actions"


            header (Authorization: 'OAuth the-session-uuid')

            clz APIChangePrimaryStorageStateMsg.class

            desc ""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "主存储的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "stateEvent"
					enclosedIn "params"
					desc "主存储的目标状态"
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("enable","disable","maintain","deleting")
				}
				column {
					name "systemTags"
 					enclosedIn "params"
 					desc ""
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
            clz APIChangePrimaryStorageStateEvent.class
        }
    }
}