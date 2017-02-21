package org.zstack.testlib

import org.zstack.sdk.AttachBackupStorageToZoneAction
import org.zstack.sdk.ZoneInventory
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Created by xing5 on 2017/2/12.
 */
class ZoneSpec implements Spec {
    String name
    String description
    List<ClusterSpec> clusters = []
    List<PrimaryStorageSpec> primaryStorage = []
    List<L2NetworkSpec> l2Networks = []
    List<VirtualRouterOfferingSpec> virtualRouterOfferingSpecs = []

    protected List<String> backupStorageToAttach = []

    ZoneInventory inventory

    ZoneSpec(String name, String description) {
        this.name = name
        this.description = description
    }

    ZoneSpec() {
    }

    ClusterSpec cluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ClusterSpec.class) Closure c) {
        def cspec = new ClusterSpec()
        c.delegate = cspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(cspec)
        clusters.add(cspec)
        return cspec
    }

    PrimaryStorageSpec nfsPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = NfsPrimaryStorageSpec.class) Closure c) {
        def nspec = new NfsPrimaryStorageSpec()
        c.delegate = nspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(nspec)
        primaryStorage.add(nspec)
        return nspec
    }

    PrimaryStorageSpec localPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = LocalStorageSpec.class) Closure c) {
        def nspec = new LocalStorageSpec()
        c.delegate = nspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(nspec)
        primaryStorage.add(nspec)
        return nspec
    }

    PrimaryStorageSpec cephPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = CephPrimaryStorageSpec.class) Closure c) {
        def nspec = new CephPrimaryStorageSpec()
        c.delegate = nspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(nspec)
        primaryStorage.add(nspec)
        return nspec
    }

    PrimaryStorageSpec smpPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SharedMountPointPrimaryStorageSpec.class) Closure c) {
        def nspec = new SharedMountPointPrimaryStorageSpec()
        c.delegate = nspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(nspec)
        primaryStorage.add(nspec)
        return nspec
    }

    L2NetworkSpec l2NoVlanNetwork(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = L2NoVlanNetworkSpec.class) Closure c) {
        def lspec = new L2NoVlanNetworkSpec()
        c.delegate = lspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(lspec)
        l2Networks.add(lspec)
        return lspec
    }

    L2NetworkSpec l2VlanNetwork(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = L2VlanNetworkSpec.class) Closure c) {
        def lspec = new L2VlanNetworkSpec()
        c.delegate = lspec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(lspec)
        l2Networks.add(lspec)
        return lspec
    }

    SecurityGroupSpec securityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = SecurityGroupSpec.class) Closure c) {
        def spec = new SecurityGroupSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    void attachBackupStorage(String...names) {
        names.each { String bsName ->
            preCreate {
                addDependency(bsName, BackupStorageSpec.class)
            }

            backupStorageToAttach.add(bsName)
        }
    }

    VirtualRouterOfferingSpec virtualRouterOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = VirtualRouterOfferingSpec.class) Closure c) {
        def spec = new VirtualRouterOfferingSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        virtualRouterOfferingSpecs.add(spec)
        return spec
    }

    EipSpec eip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = EipSpec.class) Closure c) {
        def spec = new EipSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    PortForwardingSpec portForwarding(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PortForwardingSpec.class) Closure c) {
        def spec = new PortForwardingSpec()
        c.delegate = spec
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        addChild(spec)
        return spec
    }

    LoadBalancerSpec lb(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = LoadBalancerSpec.class) Closure cl) {
        def spec = new LoadBalancerSpec()
        cl.delegate = spec
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        addChild(spec)
        return spec
    }

    SpecID create(String uuid, String sessionId) {
        inventory = createZone {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.sessionId = sessionId
            delegate.description = description
            delegate.userTags = userTags
            delegate.systemTags = systemTags
        } as ZoneInventory

        backupStorageToAttach.each { String bsName ->
            BackupStorageSpec bs = findSpec(bsName, BackupStorageSpec.class)
            def a = new AttachBackupStorageToZoneAction()
            a.zoneUuid = inventory.uuid
            a.backupStorageUuid = bs.inventory.uuid
            a.sessionId = Test.deployer.envSpec.session.uuid
            def res = a.call()
            assert res.error == null : "AttachBackupStorageToZoneAction failure: ${JSONObjectUtil.toJsonString(res.error)}"
        }

        postCreate {
            inventory = queryZone {
                conditions=["uuid=${inventory.uuid}".toString()]
            }[0]
        }

        return id(name, inventory.uuid)
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deleteZone {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }

            inventory = null
        }
    }
}
