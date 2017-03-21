package org.zstack.testlib

import org.zstack.sdk.VmInstanceInventory

/**
 * Created by xing5 on 2017/2/16.
 */
class VmSpec extends Spec implements HasSession {
    private Closure instanceOffering = {}
    private Closure image = {}
    private Closure rootDiskOffering = {}
    private Closure cluster = {}
    private Closure host = {}
    private Closure diskOfferings = {}
    private Closure l3Networks = {}
    private Closure defaultL3Network = {}
    @SpecParam(required = true)
    String name
    @SpecParam
    String description

    VmInstanceInventory inventory

    VmSpec(EnvSpec envSpec) {
        super(envSpec)
    }

    @SpecMethod
    void useInstanceOffering(String name) {
        preCreate {
            addDependency(name, InstanceOfferingSpec.class)
        }

        instanceOffering = {
            InstanceOfferingSpec spec = findSpec(name, InstanceOfferingSpec.class)
            assert spec != null: "cannot find instance offering[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useImage(String name) {
        preCreate {
            addDependency(name, ImageSpec.class)
        }

        image = {
            ImageSpec spec = findSpec(name, ImageSpec.class)
            assert spec != null: "cannot find image[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useRootDiskOffering(String name) {
        preCreate {
            addDependency(name, DiskOfferingSpec.class)
        }

        rootDiskOffering = {
            DiskOfferingSpec spec = findSpec(name, DiskOfferingSpec.class)
            assert spec != null: "cannot find useRootDiskOffering[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useCluster(String name) {
        preCreate {
            addDependency(name, ClusterSpec.class)
        }

        cluster = {
            ClusterSpec spec = findSpec(name, ClusterSpec.class)
            assert spec != null: "cannot find cluster[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useHost(String name) {
        preCreate {
            addDependency(name, HostSpec.class)
        }

        host = {
            HostSpec spec = findSpec(name, HostSpec.class)
            assert spec != null: "cannot find host[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    @SpecMethod
    void useDiskOfferings(String... names) {
        names.each { String name ->
            preCreate {
                addDependency(name, DiskOfferingSpec.class)
            }
        }

        diskOfferings = {
            return names.collect { name ->
                DiskOfferingSpec spec = findSpec(name, DiskOfferingSpec.class)
                assert spec != null: "cannot find diskOffering[$name], check the vm block of environment"
                return spec.inventory.uuid
            }
        }
    }

    @SpecMethod
    void useL3Networks(String... names) {
        names.each { String name ->
            preCreate {
                addDependency(name, L3NetworkSpec.class)
            }
        }

        l3Networks = {
            return names.collect { name ->
                L3NetworkSpec spec = findSpec(name, L3NetworkSpec.class)
                assert spec != null: "cannot find l3Network[$name], check the vm block of environment"
                return spec.inventory.uuid
            }
        }
    }

    @SpecMethod
    void useDefaultL3Network(String name) {
        preCreate {
            addDependency(name, L3NetworkSpec.class)
        }

        defaultL3Network = {
            L3NetworkSpec spec = findSpec(name, L3NetworkSpec.class)
            assert spec != null: "cannot find defaultL3Network[$name], check the vm block of environment"
            return spec.inventory.uuid
        }
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createVmInstance {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.sessionId = sessionId
            delegate.instanceOfferingUuid = instanceOffering()
            delegate.imageUuid = image()
            delegate.rootDiskOfferingUuid = rootDiskOffering()
            delegate.clusterUuid = cluster()
            delegate.hostUuid = host()
            delegate.dataDiskOfferingUuids = diskOfferings()
            delegate.l3NetworkUuids = l3Networks()
            delegate.defaultL3NetworkUuid = defaultL3Network()
        }

        postCreate {
            inventory = queryVmInstance {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            destroyVmInstance {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
