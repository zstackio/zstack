package org.zstack.header.sshkeypair

import org.zstack.header.sshkeypair.APIDetachSshKeyPairFromVmInstanceEvent

doc {
    title "DetachSshKeyPairFromVmInstance"

    category "sshKeyPair"

    desc """云主机卸载密钥对"""

    rest {
        request {
			url "DELETE /v1/ssh-key-pair/{sshKeyPairUuid}/vm-instance/{vmInstanceUuid}"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIDetachSshKeyPairFromVmInstanceMsg.class

            desc """"""
            
			params {

				column {
					name "vmInstanceUuid"
					enclosedIn ""
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "4.7.21"
				}
				column {
					name "sshKeyPairUuid"
					enclosedIn ""
					desc ""
					location "url"
					type "String"
					optional false
					since "4.7.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.7.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.7.21"
				}
			}
        }

        response {
            clz APIDetachSshKeyPairFromVmInstanceEvent.class
        }
    }
}