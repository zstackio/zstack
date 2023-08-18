package org.zstack.header.storage.addon.primary

doc {
    title "DiscoverExternalPrimaryStorage"

    category "storage.primary"

    desc """发现外部存储"""

    rest {
        request {
			url "POST /v1/primary-storage/addon/discover"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDiscoverExternalPrimaryStorageMsg.class

            desc """"""
            
			params {

				column {
					name "url"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional false
					since "4.7.11"

				}
				column {
					name "identity"
					enclosedIn ""
					desc "存储标识"
					location "body"
					type "String"
					optional true
					since "4.7.11"

				}
				column {
					name "config"
					enclosedIn ""
					desc "配置"
					location "body"
					type "String"
					optional true
					since "4.7.11"

				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.7.11"

				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.7.11"

				}
			}
        }

        response {
            clz APIDiscoverExternalPrimaryStorageEvent.class
        }
    }
}