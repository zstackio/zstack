package org.zstack.directory

import org.zstack.directory.APIRemoveResourcesFromDirectoryEvent

doc {
    title "RemoveResourcesFromDirectory"

    category "directory"

    desc """资源从指定目录中移除"""

    rest {
        request {
			url "DELETE /v1/remove/resources/directory"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIRemoveResourcesFromDirectoryMsg.class

            desc """"""
            
			params {

				column {
					name "resourceUuids"
					enclosedIn ""
					desc "批量资源UUID"
					location "body"
					type "List"
					optional false
					since "4.6.0"
					
				}
				column {
					name "directoryUuid"
					enclosedIn ""
					desc "目录UUID"
					location "body"
					type "String"
					optional false
					since "4.6.0"
					
				}
			}
        }

        response {
            clz APIRemoveResourcesFromDirectoryEvent.class
        }
    }
}