package org.zstack.testlib

import org.zstack.sdk.HostInventory

/**
 * Created by xing5 on 2017/2/12.
 */
class KVMHostSpec extends HostSpec {
    @SpecParam
    String username = "root"
    @SpecParam
    String password = "password"

    KVMHostSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = addKVMHost {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.managementIp = managementIp
            delegate.username = username
            delegate.password = password
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.clusterUuid = (parent as ClusterSpec).inventory.uuid
            delegate.sessionId = sessionId
        } as HostInventory

        postCreate {
            inventory = queryHost {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }
}
