package org.zstack.storage.surfs.primary

import org.zstack.storage.surfs.primary.APIRemoveNodeFromSurfsPrimaryStorageEvent

doc {
    title "RemoveNodeFromSurfsPrimaryStorage"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "DELETE /v1/primary-storage/surfs/{uuid}/nodes"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIRemoveNodeFromSurfsPrimaryStorageMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "nodeHostnames"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIRemoveNodeFromSurfsPrimaryStorageEvent.class
        }
    }
}