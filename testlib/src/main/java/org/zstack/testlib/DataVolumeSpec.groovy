package org.zstack.testlib

import org.zstack.sdk.VolumeInventory
/**
 * Created by mingjian.deng on 2018/1/3.
 */
class DataVolumeSpec extends Spec implements HasSession {
    @SpecParam
    String name = "data-volume"
    @SpecParam
    String description
    private Closure diskOfferings = {}
    private Closure primaryStorage = {}

    VolumeInventory inventory

    DataVolumeSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createDataVolume {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.primaryStorageUuid = primaryStorage()
            delegate.diskOfferingUuid = diskOfferings()
            delegate.description = description
            delegate.sessionId = sessionId
        }

        postCreate {
            inventory = queryVolume {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @SpecMethod
    void useDiskOffering(String name) {
        preCreate {
            addDependency(name, DiskOfferingSpec.class)
        }

        diskOfferings = {
            DiskOfferingSpec spec = findSpec(name, DiskOfferingSpec.class)
            assert spec != null: "cannot find useDiskOffering[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void usePrimaryStorage(String name) {
        preCreate {
            addDependency(name, PrimaryStorageSpec.class)
        }

        primaryStorage = {
            PrimaryStorageSpec spec = findSpec(name, PrimaryStorageSpec.class)
            assert spec != null: "cannot find usePrimaryStorage[$name], check the primaryStorage block of environment"
            return spec.inventory.uuid
        }
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteDataVolume {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
