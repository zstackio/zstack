package org.zstack.testlib

import org.zstack.utils.gson.JSONObjectUtil

trait CreationSpec {
    def errorOut(res) {
        assert res.error == null : "API failure: ${JSONObjectUtil.toJsonString(res.error)}"
        if (res.value.hasProperty("inventory")) {
            return res.value.inventory
        } else if (res.value.hasProperty("inventories")) {
            return res.value.inventories
        } else {
            return res.value
        }
    }
    
        def createUser(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createVirtualRouterOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVirtualRouterOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachPolicyToUser(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachPolicyToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryVirtualRouterVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryVirtualRouterVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateUserGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateCephPrimaryStorageMon(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateCephPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephPrimaryStorageMonAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteVmInstanceHaLevel(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmInstanceHaLevelAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachPortForwardingRule(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPortForwardingRuleAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmSshKey(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmSshKeyAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryNetworkServiceL3NetworkRef(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryAccount(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deletePolicy(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeletePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePolicyAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVersion(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVersionAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVersionAction()
        
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createL2VlanNetwork(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VlanNetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryUserTag(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserTagAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def removeVmNicFromLoadBalancer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RemoveVmNicFromLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveVmNicFromLoadBalancerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryResourcePrice(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryResourcePriceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryLdapServer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapServerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def prometheusQueryMetadata(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.PrometheusQueryMetadataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryMetadataAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getCurrentTime(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetCurrentTimeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCurrentTimeAction()
        
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getInterdependentL3NetworksImages(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetInterdependentL3NetworksImagesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetInterdependentL3NetworksImagesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def localStorageMigrateVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.LocalStorageMigrateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageMigrateVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryImage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryImageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getFreeIpOfL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetFreeIpOfL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getLocalStorageHostDiskCapacity(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetLocalStorageHostDiskCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLocalStorageHostDiskCapacityAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmConsolePassword(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsolePasswordAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getHypervisorTypes(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetHypervisorTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHypervisorTypesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryUserGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def expungeImage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ExpungeImageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeImageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachL2NetworkFromCluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachL2NetworkFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL2NetworkFromClusterAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeInstanceOfferingState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def reconnectImageStoreBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ReconnectImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectImageStoreBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addMonToCephBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddMonToCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachBackupStorageFromZone(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachBackupStorageFromZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachBackupStorageFromZoneAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createEip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryLoadBalancer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addVmNicToLoadBalancer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddVmNicToLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToLoadBalancerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def querySecurityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QuerySecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def removeMonFromFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def removeMonFromFusionstorBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeEipState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeEipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeEipStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateFusionstorPrimaryStorageMon(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryAccountResourceRef(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryAccountResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountResourceRefAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachPrimaryStorageToCluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachPrimaryStorageToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPrimaryStorageToClusterAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmMigrationCandidateHosts(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmMigrationCandidateHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmMigrationCandidateHostsAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def recoverDataVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RecoverDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverDataVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateCluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateClusterAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createSystemTag(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSystemTagAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteCluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteClusterAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getPrimaryStorageAllocatorStrategies(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteVmStaticIp(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmStaticIpAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def removeMonFromCephPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachSecurityGroupFromL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def validateSession(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ValidateSessionAction.class) Closure c) {
        def a = new org.zstack.sdk.ValidateSessionAction()
        
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateGlobalConfig(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateGlobalConfigAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createStartVmInstanceScheduler(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateStartVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStartVmInstanceSchedulerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateInstanceOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateInstanceOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getBackupStorageForCreatingImageFromVolumeSnapshot(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addIpRange(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryVolumeSnapshot(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updatePrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdatePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageCapacityAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def setVmHostname(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.SetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmHostnameAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createCluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateClusterAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def setVmInstanceHaLevel(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.SetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmInstanceHaLevelAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addNfsPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddNfsPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddNfsPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getLicenseInfo(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetLicenseInfoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseInfoAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteTag(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteTagAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteTagAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmAttachableL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmAttachableL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteHost(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteHostAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteHostAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def resumeVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ResumeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ResumeVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVolumeFormat(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVolumeFormatAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeFormatAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeHostState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeHostStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeHostStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryIpRange(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIpRangeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeClusterState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeClusterStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeClusterStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryImageStoreBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageStoreBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachNetworkServiceFromL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getBackupStorageForCreatingImageFromVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteVolumeSnapshot(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeSnapshotAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateZone(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateZoneAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachPoliciesToUser(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachPoliciesToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPoliciesToUserAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createRebootVmInstanceScheduler(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateRebootVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRebootVmInstanceSchedulerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createIPsecConnection(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateIPsecConnectionAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getPortForwardingAttachableVmNics(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetPortForwardingAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPortForwardingAttachableVmNicsAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def reloadLicense(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ReloadLicenseAction.class) Closure c) {
        def a = new org.zstack.sdk.ReloadLicenseAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteZone(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteZoneAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getCandidateZonesClustersHostsForCreatingVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def expungeVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ExpungeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeL3NetworkState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeL3NetworkStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeL3NetworkStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachDataVolumeToVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachDataVolumeToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachDataVolumeToVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachPolicyFromUser(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachPolicyFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteResourcePrice(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteResourcePriceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getCandidateVmNicForSecurityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateImageStoreBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageStoreBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeVolumeState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeVolumeStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVolumeStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateUser(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addFusionstorBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateIpRange(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateIpRangeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryL2VlanNetwork(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VlanNetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getBackupStorageCapacity(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetBackupStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageCapacityAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getBackupStorageTypes(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetBackupStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageTypesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createVolumeSnapshotScheduler(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotSchedulerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createDataVolumeFromVolumeSnapshot(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def expungeDataVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ExpungeDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeDataVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getNetworkServiceTypes(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetNetworkServiceTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNetworkServiceTypesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def logInByAccount(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.LogInByAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByAccountAction()
        
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createResourcePrice(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateResourcePriceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryFusionstorBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getLicenseCapabilities(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetLicenseCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseCapabilitiesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateImage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateImageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteVmHostname(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmHostnameAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def isReadyToGo(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.IsReadyToGoAction.class) Closure c) {
        def a = new org.zstack.sdk.IsReadyToGoAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def migrateVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.MigrateVmAction.class) Closure c) {
        def a = new org.zstack.sdk.MigrateVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteUserGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def reconnectBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ReconnectBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachEip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachEipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteUser(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addMonToCephPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddMonToCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def querySharedResource(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QuerySharedResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySharedResourceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def setVmBootOrder(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.SetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmBootOrderAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def cleanInvalidLdapBinding(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CleanInvalidLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanInvalidLdapBindingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def pauseVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.PauseVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.PauseVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryDiskOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDiskOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteVmConsolePassword(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmConsolePasswordAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createLdapBinding(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLdapBindingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteVmSshKey(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmSshKeyAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryTag(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryTagAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteDataVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachPolicyFromUserGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachPolicyFromUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryVmNicInSecurityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryVmNicInSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicInSecurityGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteLdapBinding(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapBindingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryPolicy(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryPolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPolicyAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deletePrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeletePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addLocalPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddLocalPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLocalPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addSimulatorHost(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddSimulatorHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorHostAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachPortForwardingRule(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPortForwardingRuleAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryCephPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addSftpBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSftpBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addMonToFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def querySftpBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QuerySftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySftpBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryApplianceVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryApplianceVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryApplianceVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def revokeResourceSharing(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RevokeResourceSharingAction.class) Closure c) {
        def a = new org.zstack.sdk.RevokeResourceSharingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createRootVolumeTemplateFromVolumeSnapshot(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createDataVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryL2Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createRootVolumeTemplateFromRootVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getCandidateVmForAttachingIso(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetCandidateVmForAttachingIsoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmForAttachingIsoAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteSecurityGroupRule(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupRuleAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createUserTag(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserTagAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def prometheusQueryPassThrough(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.PrometheusQueryPassThroughAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryPassThroughAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteLoadBalancer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryConsoleProxyAgent(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConsoleProxyAgentAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryVmNic(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryVmNicAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def exportImageFromBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ExportImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExportImageFromBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getCandidateIsoForAttachingVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetCandidateIsoForAttachingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateIsoForAttachingVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryQuota(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryQuotaAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def revertVolumeFromSnapshot(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RevertVolumeFromSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.RevertVolumeFromSnapshotAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteIPsecConnection(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIPsecConnectionAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def recoverImage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RecoverImageAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverImageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def backupDataVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.BackupDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.BackupDataVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteImage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteImageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteImageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteInstanceOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteInstanceOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def destroyVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DestroyVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DestroyVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachSecurityGroupToL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachSecurityGroupToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachSecurityGroupToL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addImage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddImageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeInstanceOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changePrimaryStorageState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangePrimaryStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePrimaryStorageStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def setVmStaticIp(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.SetVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmStaticIpAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateDiskOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateDiskOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryCephBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryVirtualRouterOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createLoadBalancer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createVirtualRouterVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateVirtualRouterVmAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVirtualRouterVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateLdapServer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateLdapServerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryGlobalConfig(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGlobalConfigAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryVolumeSnapshotTree(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotTreeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotTreeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createUserGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addImageStoreBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageStoreBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def checkApiPermission(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CheckApiPermissionAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckApiPermissionAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createSecurityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSecurityGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addIpRangeByNetworkCidr(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddIpRangeByNetworkCidrAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeByNetworkCidrAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteVip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteVipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def recoverVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RecoverVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmCapabilities(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmCapabilitiesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createDataVolumeFromVolumeTemplate(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteLoadBalancerListener(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerListenerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addCephBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createL2NoVlanNetwork(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateL2NoVlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2NoVlanNetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmBootOrder(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmBootOrderAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addUserToGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddUserToGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddUserToGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachL3NetworkFromVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachL3NetworkFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL3NetworkFromVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addSharedMountPointPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddSharedMountPointPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSharedMountPointPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachL2NetworkToCluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachL2NetworkToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL2NetworkToClusterAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createDiskOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDiskOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateSecurityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSecurityGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteL2Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL2NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateCephBackupStorageMon(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateCephBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephBackupStorageMonAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeResourceOwner(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeResourceOwnerAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeResourceOwnerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createVolumeSnapshot(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def logOut(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.LogOutAction.class) Closure c) {
        def a = new org.zstack.sdk.LogOutAction()
        
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def querySystemTag(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QuerySystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySystemTagAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addSimulatorPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddSimulatorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getEipAttachableVmNics(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetEipAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEipAttachableVmNicsAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def cleanUpImageCacheOnPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateHost(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateHostAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryPortForwardingRule(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPortForwardingRuleAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmHostname(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmHostnameAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def stopVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.StopVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVolumeCapabilities(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVolumeCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeCapabilitiesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def setVmConsolePassword(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.SetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmConsolePasswordAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addSimulatorBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddSimulatorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def reconnectVirtualRouter(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ReconnectVirtualRouterAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectVirtualRouterAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteDiskOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDiskOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addKVMHost(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddKVMHostAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createVip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addVmNicToSecurityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddVmNicToSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToSecurityGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachBackupStorageToZone(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachBackupStorageToZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachBackupStorageToZoneAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryLoadBalancerListener(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerListenerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeImageState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeImageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeImageStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeBackupStorageState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeBackupStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeBackupStorageStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def reconnectConsoleProxyAgent(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ReconnectConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectConsoleProxyAgentAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeDiskOfferingState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeDiskOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeDiskOfferingStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getCpuMemoryCapacity(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetCpuMemoryCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCpuMemoryCapacityAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addSecurityGroupRule(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSecurityGroupRuleAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getIpAddressCapacity(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetIpAddressCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIpAddressCapacityAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getHostAllocatorStrategies(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetHostAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHostAllocatorStrategiesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryZone(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryZoneAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryVip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryVipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteVmNicFromSecurityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteVmNicFromSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmNicFromSecurityGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def logInByLdap(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.LogInByLdapAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByLdapAction()
        
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeVipState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeVipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVipStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateVip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryLocalStorageResourceRef(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryLocalStorageResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLocalStorageResourceRefAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachPolicyToUserGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachPolicyToUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryHost(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryHostAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHostAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachPrimaryStorageFromCluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachPrimaryStorageFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPrimaryStorageFromClusterAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeVmPassword(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeVmPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVmPasswordAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def querySecurityGroupRule(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QuerySecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupRuleAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getPrimaryStorageTypes(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetPrimaryStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageTypesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def reconnectHost(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ReconnectHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectHostAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryCluster(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryClusterAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmAttachableDataVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmAttachableDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableDataVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def checkIpAvailability(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CheckIpAvailabilityAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckIpAvailabilityAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateSystemTag(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSystemTagAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def refreshLoadBalancer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RefreshLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RefreshLoadBalancerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def prometheusQueryLabelValues(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.PrometheusQueryLabelValuesAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryLabelValuesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def requestConsoleAccess(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RequestConsoleAccessAction.class) Closure c) {
        def a = new org.zstack.sdk.RequestConsoleAccessAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def logInByUser(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.LogInByUserAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByUserAction()
        
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteLdapServer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapServerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changePortForwardingRuleState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangePortForwardingRuleStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePortForwardingRuleStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def shareResource(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ShareResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.ShareResourceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deletePortForwardingRule(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeletePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePortForwardingRuleAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateSftpBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSftpBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def rebootVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RebootVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def reconnectPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ReconnectPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryLdapBinding(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapBindingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachL3NetworkToVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachL3NetworkToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL3NetworkToVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachNetworkServiceToL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachNetworkServiceToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachNetworkServiceToL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createZone(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateZoneAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryUser(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryUserAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateKVMHost(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateKVMHostAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getL3NetworkDhcpIpAddress(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetL3NetworkDhcpIpAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkDhcpIpAddressAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getL3NetworkTypes(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetL3NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkTypesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def prometheusQueryVmMonitoringData(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.PrometheusQueryVmMonitoringDataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryVmMonitoringDataAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteSecurityGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteAccount(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAccountAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def syncImageSize(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.SyncImageSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncImageSizeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeZoneState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeZoneStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeZoneStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createInstanceOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateInstanceOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createStopVmInstanceScheduler(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateStopVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStopVmInstanceSchedulerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryInstanceOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryInstanceOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmInstanceHaLevel(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmInstanceHaLevelAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateVirtualRouterOffering(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualRouterOfferingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def removeDnsFromL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RemoveDnsFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveDnsFromL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachIsoFromVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachIsoFromVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachIsoFromVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmStartingCandidateClustersHosts(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmStartingCandidateClustersHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmStartingCandidateClustersHostsAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createLoadBalancerListener(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerListenerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createDataVolumeTemplateFromVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateFusionstorBackupStorageMon(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateFusionstorBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorBackupStorageMonAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getFreeIpOfIpRange(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetFreeIpOfIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfIpRangeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getDataVolumeAttachableVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetDataVolumeAttachableVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetDataVolumeAttachableVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addDnsToL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddDnsToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDnsToL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryNetworkServiceProvider(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryNetworkServiceProviderAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceProviderAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryIPSecConnection(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryIPSecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIPSecConnectionAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def kvmRunShell(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.KvmRunShellAction.class) Closure c) {
        def a = new org.zstack.sdk.KvmRunShellAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateEip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getL2NetworkTypes(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetL2NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL2NetworkTypesAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteExportedImageFromBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteExportedImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteExportedImageFromBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteIpRange(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIpRangeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createPortForwardingRule(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePortForwardingRuleAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updatePortForwardingRule(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePortForwardingRuleAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def removeMonFromCephBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RemoveMonFromCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createL3Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL3NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getAccountQuotaUsage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetAccountQuotaUsageAction.class) Closure c) {
        def a = new org.zstack.sdk.GetAccountQuotaUsageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getCandidateVmNicsForLoadBalancer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def getVmConsoleAddress(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.GetVmConsoleAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsoleAddressAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createAccount(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateAccountAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def syncVolumeSize(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.SyncVolumeSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVolumeSizeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateQuota(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateQuotaAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def deleteEip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DeleteEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachEip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachEipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryVolume(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addCephPrimaryStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addLdapServer(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLdapServerAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def cloneVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CloneVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CloneVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateAccount(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateAccountAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def changeSecurityGroupState(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.ChangeSecurityGroupStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSecurityGroupStateAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateL2Network(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL2NetworkAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def createPolicy(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CreatePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePolicyAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def removeUserFromGroup(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.RemoveUserFromGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveUserFromGroupAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryManagementNode(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryManagementNodeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryManagementNodeAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def queryEip(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.QueryEipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEipAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachDataVolumeFromVm(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachDataVolumeFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachDataVolumeFromVmAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def addMonToFusionstorBackupStorage(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AddMonToFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorBackupStorageAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def updateVolumeSnapshot(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.UpdateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeSnapshotAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def setVmSshKey(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.SetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmSshKeyAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def detachPoliciesFromUser(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.DetachPoliciesFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPoliciesFromUserAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def startVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.StartVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def attachIsoToVmInstance(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.AttachIsoToVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachIsoToVmInstanceAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


    def calculateAccountSpending(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = org.zstack.sdk.CalculateAccountSpendingAction.class) Closure c) {
        def a = new org.zstack.sdk.CalculateAccountSpendingAction()
        a.sessionId = Test.deployer.envSpec.session?.uuid
        def code = c.rehydrate(a, this, this)
        code()
        return errorOut(a.call())
    }


}
