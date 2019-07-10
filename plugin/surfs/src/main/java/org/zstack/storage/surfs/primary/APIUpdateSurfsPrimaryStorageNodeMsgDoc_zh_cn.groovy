package org.zstack.storage.surfs.primary

import org.zstack.storage.surfs.primary.APIUpdateNodeToSurfsPrimaryStorageEvent

doc {
    title "UpdateSurfsPrimaryStorageNode"

    category "未知类别"

    desc """在这里填写API描述"""

    rest {
        request {
			url "PUT /v1/primary-storage/surfs/nodes/{nodeUuid}/actions"


            header(Authorization: 'OAuth the-session-uuid')

            clz APIUpdateSurfsPrimaryStorageNodeMsg.class

            desc """"""
            
			params {

				column {
					name "nodeUuid"
					enclosedIn "updateSurfsPrimaryStorageNode"
					desc ""
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "hostname"
					enclosedIn "updateSurfsPrimaryStorageNode"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshUsername"
					enclosedIn "updateSurfsPrimaryStorageNode"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshPassword"
					enclosedIn "updateSurfsPrimaryStorageNode"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "sshPort"
					enclosedIn "updateSurfsPrimaryStorageNode"
					desc ""
					location "body"
					type "Integer"
					optional true
					since "0.6"
					
				}
				column {
					name "nodePort"
					enclosedIn "updateSurfsPrimaryStorageNode"
					desc ""
					location "body"
					type "Integer"
					optional true
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
            clz APIUpdateNodeToSurfsPrimaryStorageEvent.class
        }
    }
}