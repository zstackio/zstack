package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/15.
 */
class VirtualRouterOfferingSpec extends InstanceOfferingSpec {
    Closure managementL3Network
    Closure publicL3Network
    private Closure image
    Closure zone
    Boolean isDefault

    // cause VirtualRouterSimulator to be loaded
    private VirtualRouterSimulator simulator = new VirtualRouterSimulator()

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = createVirtualRouterOffering {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.memorySize = memory
            delegate.cpuNum = cpu
            delegate.allocatorStrategy = allocatorStrategy
            delegate.userTags = userTags
            delegate.systemTags = systemTags
            delegate.managementNetworkUuid = managementL3Network()
            delegate.publicNetworkUuid = publicL3Network()
            delegate.imageUuid = image()
            delegate.zoneUuid = (parent as ZoneSpec).inventory.uuid
            delegate.isDefault = isDefault
        }

        postCreate {
            inventory = queryVirtualRouterOffering {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    private Closure l3Network(String name) {
        preCreate {
            addDependency(name, L3NetworkSpec.class)
        }

        return {
            L3NetworkSpec l3 = findSpec(name, L3NetworkSpec.class)
            assert l3 != null: "cannot find the L3 network[$name] defined in VirtualRouterOfferingSpec"
            return l3.inventory.uuid
        }
    }

    void useManagementL3Network(String name) {
        managementL3Network = l3Network(name)
    }

    void usePublicL3Network(String name) {
        publicL3Network = l3Network(name)
    }

    void useImage(String name) {
        preCreate {
            addDependency(name, ImageSpec.class)
        }

        image = {
            ImageSpec i = findSpec(name, ImageSpec.class)
            assert i != null: "cannot find the image[$name] defined in VirtualRouterOfferingSpec"
            return i.inventory.uuid
        }
    }
}
