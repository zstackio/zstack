package org.zstack.testlib

import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.core.Platform

trait ApiHelper {
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
    
<<<<<<< HEAD
<<<<<<< HEAD
        def addAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.AddAliyunKeySecretAction()
=======
        def getVmConsoleAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsoleAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsoleAddressAction()
>>>>>>> update sdks for hybrid
=======
        def queryImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephBackupStorageAction()
=======
    def changeEipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeEipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeEipStateAction()
>>>>>>> update sdks for hybrid
=======
    def createLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerListenerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStorageAction()
=======
    def expungeDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeDataVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def syncVirtualBorderRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStoragePoolAction()
=======
    def queryVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVniRangeAction()
>>>>>>> update sdks for hybrid
=======
    def getOssBucketFileFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOssBucketFileFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetOssBucketFileFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addConnectionAccessPointFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddConnectionAccessPointFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddConnectionAccessPointFromRemoteAction()
=======
    def queryVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVipAction()
>>>>>>> update sdks for hybrid
=======
    def createSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSystemTagAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDataCenterFromRemoteAction()
=======
    def startVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def queryCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStoragePoolAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addDnsToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDnsToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDnsToL3NetworkAction()
=======
    def getVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmHostnameAction()
>>>>>>> update sdks for hybrid
=======
    def removeDnsFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveDnsFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveDnsFromL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorBackupStorageAction()
=======
    def getBackupStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageTypesAction()
>>>>>>> update sdks for hybrid
=======
    def queryScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySchedulerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorPrimaryStorageAction()
=======
    def getL3NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkTypesAction()
>>>>>>> update sdks for hybrid
=======
    def updateLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateLdapServerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIdentityZoneFromRemoteAction()
=======
    def setVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmQgaAction()
>>>>>>> update sdks for hybrid
=======
    def createLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageAction()
=======
    def createRootVolumeTemplateFromRootVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def attachNetworkServiceToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachNetworkServiceToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachNetworkServiceToL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageStoreBackupStorageAction()
=======
    def queryScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySchedulerAction()
>>>>>>> update sdks for hybrid
=======
    def createAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateAccountAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeAction()
=======
    def getPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageCapacityAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addIpRangeByNetworkCidr(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeByNetworkCidrAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeByNetworkCidrAction()
=======
    def detachPoliciesFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPoliciesFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPoliciesFromUserAction()
>>>>>>> update sdks for hybrid
=======
    def destroyVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DestroyVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DestroyVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddKVMHostAction()
=======
    def updateVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVipAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def getVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVersionAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVersionAction()
        
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLdapServerAction()
=======
    def createSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSystemTagAction()
>>>>>>> update sdks for hybrid
=======
    def addCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStoragePoolAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addLocalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLocalPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLocalPrimaryStorageAction()
=======
    def queryPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def queryVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addMonToCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephBackupStorageAction()
=======
    def shareResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ShareResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.ShareResourceAction()
>>>>>>> update sdks for hybrid
=======
    def syncRouteEntryFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouteEntryFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouteEntryFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addMonToCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephPrimaryStorageAction()
=======
    def syncEcsImageFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsImageFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsImageFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def changeClusterState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeClusterStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeClusterStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addMonToFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorBackupStorageAction()
=======
    def getTaskProgress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTaskProgressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetTaskProgressAction()
>>>>>>> update sdks for hybrid
=======
    def queryVCenterPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addMonToFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logInByUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByUserAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByUserAction()
=======
    def logInByAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByAccountAction()
>>>>>>> update sdks
        
>>>>>>> update sdks for hybrid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addNfsPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddNfsPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddNfsPrimaryStorageAction()
=======
    def localStorageGetVolumeMigratableHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction()
>>>>>>> update sdks for hybrid
=======
    def cleanInvalidLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanInvalidLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanInvalidLdapBindingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addOssFileBucketName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddOssFileBucketNameAction.class) Closure c) {
        def a = new org.zstack.sdk.AddOssFileBucketNameAction()
=======
    def getNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNicQosAction()
>>>>>>> update sdks for hybrid
=======
    def setVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmQgaAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSecurityGroupRuleAction()
=======
    def queryEcsSecurityGroupRuleFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def updateL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSftpBackupStorageAction()
=======
    def queryLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerListenerAction()
>>>>>>> update sdks for hybrid
=======
    def getBackupStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageCapacityAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addSharedMountPointPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSharedMountPointPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSharedMountPointPrimaryStorageAction()
=======
    def createPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePortForwardingRuleAction()
>>>>>>> update sdks for hybrid
=======
    def createDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDiskOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addSimulatorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorBackupStorageAction()
=======
    def createInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateInstanceOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def createVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addSimulatorHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorHostAction()
=======
    def createSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSecurityGroupAction()
>>>>>>> update sdks for hybrid
=======
    def queryVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addSimulatorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorPrimaryStorageAction()
=======
    def detachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachAliyunKeyAction()
>>>>>>> update sdks for hybrid
=======
    def queryVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addUserToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddUserToGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddUserToGroupAction()
=======
    def checkIpAvailability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckIpAvailabilityAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckIpAvailabilityAction()
>>>>>>> update sdks for hybrid
=======
    def getCandidateZonesClustersHostsForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVCenterAction()
=======
    def changeVolumeState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVolumeStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVolumeStateAction()
>>>>>>> update sdks for hybrid
=======
    def deleteOssBucketRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssBucketRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssBucketRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addVmNicToLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToLoadBalancerAction()
=======
    def createRouterInterfacePairRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouterInterfacePairRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouterInterfacePairRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def queryL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def addVmNicToSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToSecurityGroupAction()
=======
    def refreshLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RefreshLoadBalancerAction()
>>>>>>> update sdks for hybrid
=======
    def getCandidateVmNicsForLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachAliyunKeyAction()
=======
    def queryVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterAction()
>>>>>>> update sdks for hybrid
=======
    def recoverVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachBackupStorageToZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachBackupStorageToZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachBackupStorageToZoneAction()
=======
    def reloadLicense(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReloadLicenseAction.class) Closure c) {
        def a = new org.zstack.sdk.ReloadLicenseAction()
>>>>>>> update sdks for hybrid
=======
    def attachPolicyToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPolicyToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachDataVolumeToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachDataVolumeToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachDataVolumeToVmAction()
=======
    def queryFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapBindingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachEipAction()
=======
    def detachIsoFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachIsoFromVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachIsoFromVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def createCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateClusterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachIsoToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachIsoToVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachIsoToVmInstanceAction()
=======
    def detachSecurityGroupFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def addConnectionAccessPointFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddConnectionAccessPointFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddConnectionAccessPointFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachL2NetworkToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL2NetworkToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL2NetworkToClusterAction()
=======
    def attachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPortForwardingRuleAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachL3NetworkToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL3NetworkToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL3NetworkToVmAction()
=======
    def attachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachEipAction()
>>>>>>> update sdks for hybrid
=======
    def updateScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSchedulerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachNetworkServiceToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachNetworkServiceToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachNetworkServiceToL3NetworkAction()
=======
    def attachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachAliyunKeyAction()
>>>>>>> update sdks for hybrid
=======
    def syncPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncPrimaryStorageCapacityAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachOssBucketToEcsDataCenterAction()
=======
    def deleteEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def createDataVolumeTemplateFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachPoliciesToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPoliciesToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPoliciesToUserAction()
=======
    def addAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.AddAliyunKeySecretAction()
>>>>>>> update sdks for hybrid
=======
    def queryGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGCJobAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachPolicyToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPolicyToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserAction()
=======
    def updateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def revokeResourceSharing(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevokeResourceSharingAction.class) Closure c) {
        def a = new org.zstack.sdk.RevokeResourceSharingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachPolicyToUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPolicyToUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserGroupAction()
=======
    def changeVmPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVmPasswordAction()
>>>>>>> update sdks for hybrid
=======
    def changeVipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVipStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPortForwardingRuleAction()
=======
    def createDataVolumeFromVolumeTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction()
>>>>>>> update sdks for hybrid
=======
    def getResourceNames(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceNamesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetResourceNamesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachPrimaryStorageToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPrimaryStorageToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPrimaryStorageToClusterAction()
=======
    def deleteL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL2NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def updateImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def attachSecurityGroupToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachSecurityGroupToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachSecurityGroupToL3NetworkAction()
=======
    def createL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkPoolAction()
>>>>>>> update sdks for hybrid
=======
    def changeVolumeState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVolumeStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVolumeStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def calculateAccountSpending(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateAccountSpendingAction.class) Closure c) {
        def a = new org.zstack.sdk.CalculateAccountSpendingAction()
=======
    def queryEcsInstanceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsInstanceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsInstanceFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def createL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VlanNetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeBackupStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeBackupStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeBackupStorageStateAction()
=======
    def updateUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserGroupAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmHostnameAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeClusterState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeClusterStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeClusterStateAction()
=======
    def getL3NetworkDhcpIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkDhcpIpAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkDhcpIpAddressAction()
>>>>>>> update sdks for hybrid
=======
    def createEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeDiskOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeDiskOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeDiskOfferingStateAction()
=======
    def createLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerListenerAction()
>>>>>>> update sdks for hybrid
=======
    def createStartVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStartVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStartVmInstanceSchedulerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeEipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeEipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeEipStateAction()
=======
    def addIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVirtualRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualRouterLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeHostState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeHostStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeHostStateAction()
=======
    def migrateVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MigrateVmAction.class) Closure c) {
        def a = new org.zstack.sdk.MigrateVmAction()
>>>>>>> update sdks for hybrid
=======
    def createVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeImageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeImageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeImageStateAction()
=======
    def changeSchedulerState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSchedulerStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSchedulerStateAction()
>>>>>>> update sdks for hybrid
=======
    def queryFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingAction()
=======
    def getCpuMemoryCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCpuMemoryCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCpuMemoryCapacityAction()
>>>>>>> update sdks for hybrid
=======
    def getHostAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHostAllocatorStrategiesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeInstanceOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingStateAction()
=======
    def queryVCenterPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeL3NetworkState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeL3NetworkStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeL3NetworkStateAction()
=======
    def createEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVSwitchRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def detachPolicyFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changePortForwardingRuleState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePortForwardingRuleStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePortForwardingRuleStateAction()
=======
    def createZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateZoneAction()
>>>>>>> update sdks for hybrid
=======
    def updateUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changePrimaryStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePrimaryStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePrimaryStorageStateAction()
=======
    def queryNetworkServiceProvider(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceProviderAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceProviderAction()
>>>>>>> update sdks for hybrid
=======
    def queryNotificationSubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationSubscriptionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationSubscriptionAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeResourceOwner(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeResourceOwnerAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeResourceOwnerAction()
=======
    def updateCephBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephBackupStorageMonAction()
>>>>>>> update sdks for hybrid
=======
    def deleteL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL2NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeSchedulerState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSchedulerStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSchedulerStateAction()
=======
    def getDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetDataCenterFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def detachPolicyFromUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeSecurityGroupState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSecurityGroupStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSecurityGroupStateAction()
=======
    def deleteVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmStaticIpAction()
>>>>>>> update sdks for hybrid
=======
    def prometheusQueryLabelValues(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryLabelValuesAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryLabelValuesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeVipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVipStateAction()
=======
    def updateL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL2NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def getIpAddressCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIpAddressCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIpAddressCapacityAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeVmPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVmPasswordAction()
=======
    def updateLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateLdapServerAction()
>>>>>>> update sdks for hybrid
=======
    def detachIsoFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachIsoFromVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachIsoFromVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeVolumeState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVolumeStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVolumeStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def getCurrentTime(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCurrentTimeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCurrentTimeAction()
        
>>>>>>> update sdks for hybrid
=======
    def deleteSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def changeZoneState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeZoneStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeZoneStateAction()
=======
    def deleteAllEcsInstancesFromDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction()
>>>>>>> update sdks for hybrid
=======
    def updateAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateAccountAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def checkApiPermission(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckApiPermissionAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckApiPermissionAction()
=======
    def queryIPSecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIPSecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIPSecConnectionAction()
>>>>>>> update sdks for hybrid
=======
    def deleteLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerListenerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def checkIpAvailability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckIpAvailabilityAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckIpAvailabilityAction()
=======
    def deleteVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeQosAction()
>>>>>>> update sdks for hybrid
=======
    def getDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetDataCenterFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def cleanInvalidLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanInvalidLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanInvalidLdapBindingAction()
=======
    def deletePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def queryEcsInstanceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsInstanceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsInstanceFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def cleanUpImageCacheOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction()
=======
    def syncEcsInstanceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsInstanceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsInstanceFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def queryConnectionAccessPointFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConnectionAccessPointFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConnectionAccessPointFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def cloneVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CloneVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CloneVmInstanceAction()
=======
    def getVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmBootOrderAction()
>>>>>>> update sdks for hybrid
=======
    def changeSecurityGroupState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSecurityGroupStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSecurityGroupStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateAccountAction()
=======
    def queryCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStoragePoolAction()
>>>>>>> update sdks for hybrid
=======
    def createEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createBaremetalChessis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalChessisAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateBaremetalChessisAction()
=======
    def createStartVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStartVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStartVmInstanceSchedulerAction()
>>>>>>> update sdks for hybrid
=======
    def updateCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateClusterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateBaremetalHostCfgAction()
=======
    def queryL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VlanNetworkAction()
>>>>>>> update sdks for hybrid
=======
    def updateSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSftpBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateBaremetalPxeServerAction()
=======
    def updateScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSchedulerAction()
>>>>>>> update sdks for hybrid
=======
    def queryEcsVSwitchFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVSwitchFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVSwitchFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateClusterAction()
=======
    def prometheusQueryVmMonitoringData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryVmMonitoringDataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryVmMonitoringDataAction()
>>>>>>> update sdks for hybrid
=======
    def queryDataCenterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDataCenterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDataCenterFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeAction()
=======
    def deleteEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def deleteLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createDataVolumeFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction()
=======
    def removeMonFromCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def createDataVolumeFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createDataVolumeFromVolumeTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction()
=======
    def createRebootVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRebootVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRebootVmInstanceSchedulerAction()
>>>>>>> update sdks for hybrid
=======
    def attachSecurityGroupToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachSecurityGroupToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachSecurityGroupToL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createDataVolumeTemplateFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction()
=======
    def syncRouterInterfaceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouterInterfaceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouterInterfaceFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def queryVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDiskOfferingAction()
=======
    def createUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserTagAction()
>>>>>>> update sdks for hybrid
=======
    def updateVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeSnapshotAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createEcsImageFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsImageFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsImageFromLocalImageAction()
=======
    def deleteDataCenterInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataCenterInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataCenterInLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logInByLdap(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByLdapAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByLdapAction()
        
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createEcsInstanceFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsInstanceFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsInstanceFromLocalImageAction()
=======
    def deleteVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVCenterAction()
>>>>>>> update sdks for hybrid
=======
    def getTaskProgress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTaskProgressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetTaskProgressAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRemoteAction()
=======
    def queryManagementNode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryManagementNodeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryManagementNodeAction()
>>>>>>> update sdks for hybrid
=======
    def updateQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateQuotaAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction()
=======
    def updateVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def updatePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePortForwardingRuleAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVSwitchRemoteAction()
=======
    def deletePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePortForwardingRuleAction()
>>>>>>> update sdks for hybrid
=======
    def queryAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAliyunKeySecretAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVpcRemoteAction()
=======
    def updateRouteInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateRouteInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateRouteInterfaceRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def addVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVCenterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEipAction()
=======
    def getOssBucketNameFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOssBucketNameFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetOssBucketNameFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def queryEcsImageFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsImageFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsImageFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateIPsecConnectionAction()
=======
    def getNetworkServiceTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNetworkServiceTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNetworkServiceTypesAction()
>>>>>>> update sdks for hybrid
=======
    def changeHostState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeHostStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeHostStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateInstanceOfferingAction()
=======
    def resumeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResumeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ResumeVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def getEipAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEipAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEipAttachableVmNicsAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createL2NoVlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2NoVlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2NoVlanNetworkAction()
=======
    def deleteEcsVSwitchInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchInLocalAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVmNicFromSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmNicFromSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmNicFromSecurityGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VlanNetworkAction()
=======
    def setNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetNicQosAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVniRangeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkAction()
=======
    def prometheusQueryLabelValues(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryLabelValuesAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryLabelValuesAction()
>>>>>>> update sdks for hybrid
=======
    def changeSchedulerState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSchedulerStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSchedulerStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkPoolAction()
=======
    def queryQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryQuotaAction()
>>>>>>> update sdks for hybrid
=======
    def getLicenseCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLicenseCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseCapabilitiesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL3NetworkAction()
=======
    def reimageVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReimageVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ReimageVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def changeInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLdapBindingAction()
=======
    def getFreeIpOfL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def createVolumeSnapshotScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotSchedulerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerAction()
=======
    def queryApplianceVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryApplianceVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryApplianceVmAction()
>>>>>>> update sdks for hybrid
=======
    def addSharedMountPointPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSharedMountPointPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSharedMountPointPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerListenerAction()
=======
    def detachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPortForwardingRuleAction()
>>>>>>> update sdks for hybrid
=======
    def pauseVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PauseVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.PauseVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePolicyAction()
=======
    def syncVirtualRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualRouterFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def getAccountQuotaUsage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAccountQuotaUsageAction.class) Closure c) {
        def a = new org.zstack.sdk.GetAccountQuotaUsageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePortForwardingRuleAction()
=======
    def updateZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateZoneAction()
>>>>>>> update sdks for hybrid
=======
    def createEcsInstanceFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsInstanceFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsInstanceFromLocalImageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createRebootVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRebootVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRebootVmInstanceSchedulerAction()
=======
    def getVmStartingCandidateClustersHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmStartingCandidateClustersHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmStartingCandidateClustersHostsAction()
>>>>>>> update sdks for hybrid
=======
    def queryConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConsoleProxyAgentAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateResourcePriceAction()
=======
    def queryL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def queryVirtualBorderRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createRootVolumeTemplateFromRootVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction()
=======
    def deleteVirtualBorderRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualBorderRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualBorderRouterLocalAction()
>>>>>>> update sdks for hybrid
=======
    def getPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageCapacityAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createRootVolumeTemplateFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction()
=======
    def queryConnectionAccessPointFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConnectionAccessPointFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConnectionAccessPointFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def cleanUpImageCacheOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createRouteEntryForConnectionRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction()
=======
    def createLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLdapBindingAction()
>>>>>>> update sdks for hybrid
=======
    def deleteHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHostAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteHostAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createRouterInterfacePairRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouterInterfacePairRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouterInterfacePairRemoteAction()
=======
    def triggerGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TriggerGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.TriggerGCJobAction()
>>>>>>> update sdks for hybrid
=======
    def queryResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryResourcePriceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSecurityGroupAction()
=======
    def createRouteEntryForConnectionRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def updateCephPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephPrimaryStorageMonAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createStartVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStartVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStartVmInstanceSchedulerAction()
=======
    def cleanInvalidLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanInvalidLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanInvalidLdapBindingAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmConsolePasswordAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createStopVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStopVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStopVmInstanceSchedulerAction()
=======
    def syncVirtualBorderRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def calculateAccountSpending(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateAccountSpendingAction.class) Closure c) {
        def a = new org.zstack.sdk.CalculateAccountSpendingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSystemTagAction()
=======
    def deleteEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def deleteImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteImageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteImageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserAction()
=======
    def deleteNotifications(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNotificationsAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNotificationsAction()
>>>>>>> update sdks for hybrid
=======
    def queryL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserGroupAction()
=======
    def rebootVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def getCandidateVmForAttachingIso(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmForAttachingIsoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmForAttachingIsoAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserTagAction()
=======
    def queryVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
=======
    def changeEipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeEipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeEipStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVipAction()
=======
    def getCandidateVmNicForSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction()
>>>>>>> update sdks for hybrid
=======
    def getVolumeCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeCapabilitiesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVirtualRouterOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logOut(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogOutAction.class) Closure c) {
        def a = new org.zstack.sdk.LogOutAction()
        
>>>>>>> update sdks for hybrid
=======
    def createEcsImageFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsImageFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsImageFromLocalImageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVmInstanceAction()
=======
    def getAccountQuotaUsage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAccountQuotaUsageAction.class) Closure c) {
        def a = new org.zstack.sdk.GetAccountQuotaUsageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteClusterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVniRangeAction()
=======
    def deleteIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIpRangeAction()
>>>>>>> update sdks for hybrid
=======
    def createDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotAction()
=======
    def queryVCenterDatacenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterDatacenterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterDatacenterAction()
>>>>>>> update sdks for hybrid
=======
    def addVmNicToLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToLoadBalancerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createVolumeSnapshotScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotSchedulerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logInByLdap(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByLdapAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByLdapAction()
        
>>>>>>> update sdks for hybrid
=======
    def queryNetworkServiceL3NetworkRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateWebhookAction()
=======
    def removeUserFromGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveUserFromGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveUserFromGroupAction()
>>>>>>> update sdks for hybrid
=======
    def queryHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHostAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def createZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateZoneAction()
=======
    def deleteLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapBindingAction()
>>>>>>> update sdks for hybrid
=======
    def createL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAccountAction()
=======
    def queryL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkAction()
>>>>>>> update sdks for hybrid
=======
    def createRouteEntryForConnectionRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAliyunKeySecretAction()
=======
    def queryCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def queryVolumeSnapshotTree(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotTreeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotTreeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteAllEcsInstancesFromDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction()
=======
    def addSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSftpBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def queryRouterInterfaceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryRouterInterfaceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryRouterInterfaceFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBackupStorageAction()
=======
    def requestConsoleAccess(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RequestConsoleAccessAction.class) Closure c) {
        def a = new org.zstack.sdk.RequestConsoleAccessAction()
>>>>>>> update sdks for hybrid
=======
    def queryImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageStoreBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteBaremetalChessis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalChessisAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBaremetalChessisAction()
=======
    def deleteL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def queryVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBaremetalHostCfgAction()
=======
    def createEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsImageLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBaremetalPxeServerAction()
=======
    def getCandidateIsoForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateIsoForAttachingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateIsoForAttachingVmAction()
>>>>>>> update sdks for hybrid
=======
    def syncVirtualRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualRouterFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteCephPrimaryStoragePoolAction()
=======
    def changeDiskOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeDiskOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeDiskOfferingStateAction()
>>>>>>> update sdks for hybrid
=======
    def syncEcsVpcFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVpcFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVpcFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteClusterAction()
=======
    def queryAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAliyunKeySecretAction()
>>>>>>> update sdks for hybrid
=======
    def requestConsoleAccess(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RequestConsoleAccessAction.class) Closure c) {
        def a = new org.zstack.sdk.RequestConsoleAccessAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteConnectionAccessPointLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteConnectionAccessPointLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteConnectionAccessPointLocalAction()
=======
    def queryVirtualRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def detachL2NetworkFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL2NetworkFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL2NetworkFromClusterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteDataCenterInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataCenterInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataCenterInLocalAction()
=======
    def updateDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateDiskOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def queryVCenterBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataVolumeAction()
=======
    def queryIdentityZoneFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIdentityZoneFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIdentityZoneFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def createDataVolumeFromVolumeTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDiskOfferingAction()
=======
    def changeZoneState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeZoneStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeZoneStateAction()
>>>>>>> update sdks for hybrid
=======
    def reconnectHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectHostAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsImageLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageLocalAction()
=======
    def createLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerAction()
>>>>>>> update sdks for hybrid
=======
    def updateDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateDiskOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsImageRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageRemoteAction()
=======
    def addFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def recoverImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverImageAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverImageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsInstanceAction()
=======
    def attachL2NetworkToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL2NetworkToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL2NetworkToClusterAction()
>>>>>>> update sdks for hybrid
=======
    def updateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsSecurityGroupInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction()
=======
    def deleteRouteEntryRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouteEntryRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouteEntryRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def localStorageMigrateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageMigrateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageMigrateVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction()
=======
    def createEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVpcRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def updateL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL2NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction()
=======
    def queryEcsVSwitchFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVSwitchFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVSwitchFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def queryVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsVSwitchInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchInLocalAction()
=======
    def updateNotificationsStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateNotificationsStatusAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateNotificationsStatusAction()
>>>>>>> update sdks for hybrid
=======
    def deleteIdentityZoneInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIdentityZoneInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIdentityZoneInLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchRemoteAction()
=======
    def getBackupStorageForCreatingImageFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def setImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetImageQgaAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsVpcInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcInLocalAction()
=======
    def changeHostState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeHostStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeHostStateAction()
>>>>>>> update sdks for hybrid
=======
    def reloadLicense(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReloadLicenseAction.class) Closure c) {
        def a = new org.zstack.sdk.ReloadLicenseAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcRemoteAction()
=======
    def getInterdependentL3NetworksImages(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetInterdependentL3NetworksImagesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetInterdependentL3NetworksImagesAction()
>>>>>>> update sdks for hybrid
=======
    def queryCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEipAction()
=======
    def deleteTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteTagAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteTagAction()
>>>>>>> update sdks for hybrid
=======
    def getVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeQosAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteExportedImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteExportedImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteExportedImageFromBackupStorageAction()
=======
    def deleteVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmSshKeyAction()
>>>>>>> update sdks for hybrid
=======
    def attachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachEipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteGCJobAction()
=======
    def addVmNicToSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToSecurityGroupAction()
>>>>>>> update sdks for hybrid
=======
    def addImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHostAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteHostAction()
=======
    def calculateAccountSpending(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateAccountSpendingAction.class) Closure c) {
        def a = new org.zstack.sdk.CalculateAccountSpendingAction()
>>>>>>> update sdks for hybrid
=======
    def createStopVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStopVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStopVmInstanceSchedulerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIPsecConnectionAction()
=======
    def deleteRouterInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def queryEcsSecurityGroupFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteIdentityZoneInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIdentityZoneInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIdentityZoneInLocalAction()
=======
    def getLicenseCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLicenseCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseCapabilitiesAction()
>>>>>>> update sdks for hybrid
=======
    def addLocalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLocalPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLocalPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteImageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteImageAction()
=======
    def deleteVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
=======
    def deleteInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteInstanceOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteInstanceOfferingAction()
=======
    def deleteImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteImageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteImageAction()
>>>>>>> update sdks for hybrid
=======
    def createRootVolumeTemplateFromRootVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIpRangeAction()
=======
    def queryVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicAction()
>>>>>>> update sdks for hybrid
=======
    def queryVmNicInSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicInSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicInSecurityGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL2NetworkAction()
=======
    def removeMonFromFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def createL2NoVlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2NoVlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2NoVlanNetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL3NetworkAction()
=======
    def getVolumeFormat(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeFormatAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeFormatAction()
>>>>>>> update sdks for hybrid
=======
    def detachNetworkServiceFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapBindingAction()
=======
    def setVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmSshKeyAction()
>>>>>>> update sdks for hybrid
=======
    def deleteLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapServerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapServerAction()
=======
    def changeVipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVipStateAction()
>>>>>>> update sdks for hybrid
=======
    def createSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSecurityGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerAction()
=======
    def removeMonFromFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def removeMonFromCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerListenerAction()
=======
    def exportImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExportImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExportImageFromBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def addDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDataCenterFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNicQosAction()
=======
    def deleteCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteClusterAction()
>>>>>>> update sdks for hybrid
=======
    def deleteScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSchedulerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteNotifications(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNotificationsAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNotificationsAction()
=======
    def queryNotification(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationAction()
>>>>>>> update sdks for hybrid
=======
    def removeMonFromCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteOssFileBucketNameInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssFileBucketNameInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssFileBucketNameInLocalAction()
=======
    def deleteLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerListenerAction()
>>>>>>> update sdks for hybrid
=======
    def revertVolumeFromSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevertVolumeFromSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.RevertVolumeFromSnapshotAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deletePolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePolicyAction()
=======
    def queryHybridEipFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHybridEipFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHybridEipFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def createRebootVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRebootVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRebootVmInstanceSchedulerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deletePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePortForwardingRuleAction()
=======
    def localStorageMigrateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageMigrateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageMigrateVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def reconnectConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectConsoleProxyAgentAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deletePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePrimaryStorageAction()
=======
    def queryVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def detachL3NetworkFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL3NetworkFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL3NetworkFromVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteResourcePriceAction()
=======
    def updateEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEipAction()
>>>>>>> update sdks for hybrid
=======
    def queryBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteRouteEntryRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouteEntryRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouteEntryRemoteAction()
=======
    def queryVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def getImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetImageQgaAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteRouterInterfaceLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceLocalAction()
=======
    def deleteZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteZoneAction()
>>>>>>> update sdks for hybrid
=======
    def updateUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteRouterInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceRemoteAction()
=======
    def updateCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateClusterAction()
>>>>>>> update sdks for hybrid
=======
    def queryCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryClusterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSchedulerAction()
=======
    def deleteRouterInterfaceLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceLocalAction()
>>>>>>> update sdks for hybrid
=======
    def stopEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopEcsInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupAction()
=======
    def updateL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupRuleAction()
=======
    def addSimulatorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def removeMonFromFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteTagAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteTagAction()
=======
    def setImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetImageQgaAction()
>>>>>>> update sdks for hybrid
=======
    def localStorageGetVolumeMigratableHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserAction()
=======
    def createEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEipAction()
>>>>>>> update sdks for hybrid
=======
    def deleteAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAccountAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserGroupAction()
=======
    def setVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmStaticIpAction()
>>>>>>> update sdks for hybrid
=======
    def syncEcsImageFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsImageFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsImageFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVCenterAction()
=======
    def addSimulatorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def isReadyToGo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsReadyToGoAction.class) Closure c) {
        def a = new org.zstack.sdk.IsReadyToGoAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVipAction()
=======
    def updateIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateIpRangeAction()
>>>>>>> update sdks for hybrid
=======
    def createUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVirtualBorderRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualBorderRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualBorderRouterLocalAction()
=======
    def createVolumeSnapshotScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotSchedulerAction()
>>>>>>> update sdks for hybrid
=======
    def prometheusQueryMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryMetadataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryMetadataAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVirtualRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualRouterLocalAction()
=======
    def detachL3NetworkFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL3NetworkFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL3NetworkFromVmAction()
>>>>>>> update sdks for hybrid
=======
    def detachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachEipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmConsolePasswordAction()
=======
    def changeL3NetworkState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeL3NetworkStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeL3NetworkStateAction()
>>>>>>> update sdks for hybrid
=======
    def refreshLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RefreshLoadBalancerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmHostnameAction()
=======
    def detachDataVolumeFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachDataVolumeFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachDataVolumeFromVmAction()
>>>>>>> update sdks for hybrid
=======
    def getPortForwardingAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPortForwardingAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPortForwardingAttachableVmNicsAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmInstanceHaLevelAction()
=======
    def createL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VlanNetworkAction()
>>>>>>> update sdks for hybrid
=======
    def getNetworkServiceTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNetworkServiceTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNetworkServiceTypesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVmNicFromSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmNicFromSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmNicFromSecurityGroupAction()
=======
    def queryDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDiskOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def getVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsolePasswordAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmSshKeyAction()
=======
    def deleteDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def querySftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySftpBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmStaticIpAction()
=======
    def addLocalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLocalPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLocalPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteNotifications(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNotificationsAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNotificationsAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVniRangeAction()
=======
    def createUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserAction()
>>>>>>> update sdks for hybrid
=======
    def updateFusionstorPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeQosAction()
=======
    def detachPolicyFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserAction()
>>>>>>> update sdks for hybrid
=======
    def getVmAttachableDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableDataVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeSnapshotAction()
=======
    def deleteIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIPsecConnectionAction()
>>>>>>> update sdks for hybrid
=======
    def stopVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteWebhookAction()
=======
    def prometheusQueryMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryMetadataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryMetadataAction()
>>>>>>> update sdks for hybrid
=======
    def setVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmConsolePasswordAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def deleteZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteZoneAction()
=======
    def queryVolumeSnapshotTree(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotTreeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotTreeAction()
>>>>>>> update sdks for hybrid
=======
    def getVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmInstanceHaLevelAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def destroyVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DestroyVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DestroyVmInstanceAction()
=======
    def getVmAttachableDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableDataVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def getL3NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkTypesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachAliyunKeyAction()
=======
    def addMonToFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def attachL3NetworkToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL3NetworkToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL3NetworkToVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachBackupStorageFromZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachBackupStorageFromZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachBackupStorageFromZoneAction()
=======
    def changeBackupStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeBackupStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeBackupStorageStateAction()
>>>>>>> update sdks for hybrid
=======
    def detachDataVolumeFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachDataVolumeFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachDataVolumeFromVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachDataVolumeFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachDataVolumeFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachDataVolumeFromVmAction()
=======
    def syncEcsVpcFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVpcFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVpcFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def getVolumeFormat(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeFormatAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeFormatAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachEipAction()
=======
    def queryLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapBindingAction()
>>>>>>> update sdks for hybrid
=======
    def createEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVpcRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachIsoFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachIsoFromVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachIsoFromVmInstanceAction()
=======
    def detachL2NetworkFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL2NetworkFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL2NetworkFromClusterAction()
>>>>>>> update sdks for hybrid
=======
    def queryOssBucketFileName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryOssBucketFileNameAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryOssBucketFileNameAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachL2NetworkFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL2NetworkFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL2NetworkFromClusterAction()
=======
    def createDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def queryL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachL3NetworkFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL3NetworkFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL3NetworkFromVmAction()
=======
    def addConnectionAccessPointFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddConnectionAccessPointFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddConnectionAccessPointFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def attachIsoToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachIsoToVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachIsoToVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachNetworkServiceFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction()
=======
    def getIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIdentityZoneFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def getLicenseInfo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLicenseInfoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseInfoAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachOssBucketToEcsDataCenterAction()
=======
    def createCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateClusterAction()
>>>>>>> update sdks for hybrid
=======
    def checkApiPermission(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckApiPermissionAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckApiPermissionAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachPoliciesFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPoliciesFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPoliciesFromUserAction()
=======
    def getVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeQosAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def getCurrentTime(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCurrentTimeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCurrentTimeAction()
        
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachPolicyFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserAction()
=======
    def updateFusionstorBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorBackupStorageMonAction()
>>>>>>> update sdks for hybrid
=======
    def addImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageStoreBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachPolicyFromUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserGroupAction()
=======
    def addCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def updateZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateZoneAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPortForwardingRuleAction()
=======
    def getCandidateVmForAttachingIso(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmForAttachingIsoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmForAttachingIsoAction()
>>>>>>> update sdks for hybrid
=======
    def attachL2NetworkToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL2NetworkToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL2NetworkToClusterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachPrimaryStorageFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPrimaryStorageFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPrimaryStorageFromClusterAction()
=======
    def prometheusQueryVmMonitoringData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryVmMonitoringDataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryVmMonitoringDataAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def detachSecurityGroupFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction()
=======
    def getHypervisorTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHypervisorTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHypervisorTypesAction()
>>>>>>> update sdks for hybrid
=======
    def queryQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryQuotaAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def exportImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExportImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExportImageFromBackupStorageAction()
=======
    def createIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateIPsecConnectionAction()
>>>>>>> update sdks for hybrid
=======
    def queryEcsVpcFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVpcFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVpcFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def expungeDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeDataVolumeAction()
=======
    def getPortForwardingAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPortForwardingAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPortForwardingAttachableVmNicsAction()
>>>>>>> update sdks for hybrid
=======
    def createLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLdapBindingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def expungeImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeImageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeImageAction()
=======
    def setVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmInstanceHaLevelAction()
>>>>>>> update sdks for hybrid
=======
    def migrateVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MigrateVmAction.class) Closure c) {
        def a = new org.zstack.sdk.MigrateVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def expungeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeVmInstanceAction()
=======
    def addVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVCenterAction()
>>>>>>> update sdks for hybrid
=======
    def queryVirtualRouterVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getAccountQuotaUsage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAccountQuotaUsageAction.class) Closure c) {
        def a = new org.zstack.sdk.GetAccountQuotaUsageAction()
=======
    def changeResourceOwner(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeResourceOwnerAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeResourceOwnerAction()
>>>>>>> update sdks for hybrid
=======
    def updateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getBackupStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageCapacityAction()
=======
    def getVolumeCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeCapabilitiesAction()
>>>>>>> update sdks for hybrid
=======
    def createInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateInstanceOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getBackupStorageForCreatingImageFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction()
=======
    def deleteIdentityZoneInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIdentityZoneInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIdentityZoneInLocalAction()
>>>>>>> update sdks for hybrid
=======
    def queryRouteEntryFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryRouteEntryFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryRouteEntryFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getBackupStorageForCreatingImageFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction()
=======
    def deleteOssFileBucketNameInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssFileBucketNameInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssFileBucketNameInLocalAction()
>>>>>>> update sdks for hybrid
=======
    def deletePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getBackupStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageTypesAction()
=======
    def expungeImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeImageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeImageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getCandidateIsoForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateIsoForAttachingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateIsoForAttachingVmAction()
=======
    def createStopVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStopVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStopVmInstanceSchedulerAction()
>>>>>>> update sdks for hybrid
=======
    def exportImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExportImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExportImageFromBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getCandidateVmForAttachingIso(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmForAttachingIsoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmForAttachingIsoAction()
=======
    def queryLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapServerAction()
>>>>>>> update sdks for hybrid
=======
    def expungeDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeDataVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getCandidateVmNicForSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction()
=======
    def queryBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getCandidateVmNicsForLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction()
=======
    def queryVirtualBorderRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def updateBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getCandidateZonesClustersHostsForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction()
=======
    def createL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkAction()
>>>>>>> update sdks for hybrid
=======
    def getLocalStorageHostDiskCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLocalStorageHostDiskCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLocalStorageHostDiskCapacityAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getCpuMemoryCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCpuMemoryCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCpuMemoryCapacityAction()
=======
    def reconnectBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteConnectionAccessPointLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteConnectionAccessPointLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteConnectionAccessPointLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getCurrentTime(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCurrentTimeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCurrentTimeAction()
        
=======
    def reconnectImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectImageStoreBackupStorageAction()
=======
    def queryPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPolicyAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks for hybrid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetDataCenterFromRemoteAction()
=======
    def deleteNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNicQosAction()
>>>>>>> update sdks for hybrid
=======
    def setVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmSshKeyAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteResourcePriceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getEcsInstanceVncUrl(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEcsInstanceVncUrlAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEcsInstanceVncUrlAction()
=======
    def getVmAttachableL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def querySecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupRuleAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getEipAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEipAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEipAttachableVmNicsAction()
=======
    def terminateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def getCpuMemoryCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCpuMemoryCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCpuMemoryCapacityAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getFreeIpOfIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfIpRangeAction()
=======
    def addLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLdapServerAction()
>>>>>>> update sdks for hybrid
=======
    def addMonToFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getFreeIpOfL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfL3NetworkAction()
=======
    def addCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def queryIdentityZoneFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIdentityZoneFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIdentityZoneFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getHostAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHostAllocatorStrategiesAction()
=======
    def deleteResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteResourcePriceAction()
>>>>>>> update sdks for hybrid
=======
    def queryVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getHypervisorTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHypervisorTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHypervisorTypesAction()
=======
    def deleteLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapServerAction()
>>>>>>> update sdks for hybrid
=======
    def getDataVolumeAttachableVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDataVolumeAttachableVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetDataVolumeAttachableVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIdentityZoneFromRemoteAction()
=======
    def deleteBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def getEcsInstanceVncUrl(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEcsInstanceVncUrlAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEcsInstanceVncUrlAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetImageQgaAction()
=======
    def attachPoliciesToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPoliciesToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPoliciesToUserAction()
>>>>>>> update sdks for hybrid
=======
    def queryVCenterCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterClusterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getInterdependentL3NetworksImages(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetInterdependentL3NetworksImagesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetInterdependentL3NetworksImagesAction()
=======
    def getBackupStorageForCreatingImageFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getIpAddressCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIpAddressCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIpAddressCapacityAction()
=======
    def addFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def queryLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getL2NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL2NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL2NetworkTypesAction()
=======
    def getImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetImageQgaAction()
>>>>>>> update sdks for hybrid
=======
    def setVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmHostnameAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getL3NetworkDhcpIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkDhcpIpAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkDhcpIpAddressAction()
=======
    def deleteEcsVpcInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcInLocalAction()
>>>>>>> update sdks for hybrid
=======
    def detachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPortForwardingRuleAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getL3NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkTypesAction()
=======
    def deleteVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVipAction()
>>>>>>> update sdks for hybrid
=======
    def queryNetworkServiceProvider(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceProviderAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceProviderAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getLicenseCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLicenseCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseCapabilitiesAction()
=======
    def attachIsoToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachIsoToVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachIsoToVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def attachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPortForwardingRuleAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def expungeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getLocalStorageHostDiskCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLocalStorageHostDiskCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLocalStorageHostDiskCapacityAction()
=======
    def addMonToCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeQosAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getNetworkServiceTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNetworkServiceTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNetworkServiceTypesAction()
=======
    def deleteDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDiskOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def getFreeIpOfIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfIpRangeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNicQosAction()
=======
    def queryEcsSecurityGroupFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def deleteAllEcsInstancesFromDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getOssBucketNameFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOssBucketNameFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetOssBucketNameFromRemoteAction()
=======
    def reclaimSpaceFromImageStore(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReclaimSpaceFromImageStoreAction.class) Closure c) {
        def a = new org.zstack.sdk.ReclaimSpaceFromImageStoreAction()
>>>>>>> update sdks for hybrid
=======
    def deleteDataCenterInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataCenterInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataCenterInLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getPortForwardingAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPortForwardingAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPortForwardingAttachableVmNicsAction()
=======
    def addNfsPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddNfsPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddNfsPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmStaticIpAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getPrimaryStorageAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction()
=======
    def queryVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def createVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageCapacityAction()
=======
    def cloneVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CloneVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CloneVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def recoveryVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getPrimaryStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageTypesAction()
=======
    def reconnectHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectHostAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsImageRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getResourceNames(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceNamesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetResourceNamesAction()
=======
    def deleteGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteGCJobAction()
>>>>>>> update sdks for hybrid
=======
    def queryCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getTaskProgress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTaskProgressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetTaskProgressAction()
=======
    def attachDataVolumeToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachDataVolumeToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachDataVolumeToVmAction()
>>>>>>> update sdks for hybrid
=======
    def deleteSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupRuleAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVersionAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVersionAction()
        
=======
    def addCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStoragePoolAction()
=======
    def addKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddKVMHostAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks for hybrid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmAttachableDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableDataVolumeAction()
=======
    def deleteInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteInstanceOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def detachPoliciesFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPoliciesFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPoliciesFromUserAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmAttachableL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableL3NetworkAction()
=======
    def querySftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySftpBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def queryUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmBootOrderAction()
=======
    def queryImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageAction()
>>>>>>> update sdks for hybrid
=======
    def updateSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSystemTagAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmCapabilitiesAction()
=======
    def getFreeIpOfIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfIpRangeAction()
>>>>>>> update sdks for hybrid
=======
    def changeBackupStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeBackupStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeBackupStorageStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmConsoleAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsoleAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsoleAddressAction()
=======
    def createVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
=======
    def queryIPSecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIPSecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIPSecConnectionAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsolePasswordAction()
=======
    def addKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddKVMHostAction()
>>>>>>> update sdks for hybrid
=======
    def deleteGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteGCJobAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmHostnameAction()
=======
    def attachNetworkServiceToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachNetworkServiceToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachNetworkServiceToL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def queryLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapBindingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmInstanceHaLevelAction()
=======
    def updateUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserAction()
>>>>>>> update sdks for hybrid
=======
    def addMonToCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmMigrationCandidateHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmMigrationCandidateHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmMigrationCandidateHostsAction()
=======
    def addImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageAction()
>>>>>>> update sdks for hybrid
=======
    def createL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmQgaAction()
=======
    def syncEcsSecurityGroupFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def expungeImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeImageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeImageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmSshKeyAction()
=======
    def queryImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageStoreBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def addSimulatorHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorHostAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVmStartingCandidateClustersHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmStartingCandidateClustersHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmStartingCandidateClustersHostsAction()
=======
    def detachNetworkServiceFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def startVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVolumeCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeCapabilitiesAction()
=======
    def queryPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPolicyAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logOut(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogOutAction.class) Closure c) {
        def a = new org.zstack.sdk.LogOutAction()
        
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVolumeFormat(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeFormatAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeFormatAction()
=======
    def deleteVirtualRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualRouterLocalAction()
>>>>>>> update sdks for hybrid
=======
    def reconnectPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def getVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeQosAction()
=======
    def deleteCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteCephPrimaryStoragePoolAction()
>>>>>>> update sdks for hybrid
=======
    def updateVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def isReadyToGo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsReadyToGoAction.class) Closure c) {
        def a = new org.zstack.sdk.IsReadyToGoAction()
=======
    def getResourceNames(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceNamesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetResourceNamesAction()
>>>>>>> update sdks for hybrid
=======
    def addDnsToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDnsToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDnsToL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def kvmRunShell(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.KvmRunShellAction.class) Closure c) {
        def a = new org.zstack.sdk.KvmRunShellAction()
=======
    def updateSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSftpBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def changeL3NetworkState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeL3NetworkStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeL3NetworkStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def localStorageGetVolumeMigratableHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction()
=======
    def createDataVolumeFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logInByUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByUserAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByUserAction()
        
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def localStorageMigrateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageMigrateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageMigrateVolumeAction()
=======
    def attachL3NetworkToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL3NetworkToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL3NetworkToVmAction()
>>>>>>> update sdks for hybrid
=======
    def changeDiskOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeDiskOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeDiskOfferingStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def logInByAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByAccountAction()
        
=======
    def queryZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryZoneAction()
=======
    def queryLocalStorageResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLocalStorageResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLocalStorageResourceRefAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks for hybrid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def logInByLdap(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByLdapAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByLdapAction()
        
=======
    def queryRouterInterfaceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryRouterInterfaceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryRouterInterfaceFromLocalAction()
=======
    def detachSecurityGroupFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks for hybrid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def logInByUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByUserAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByUserAction()
        
=======
    def getVmCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmCapabilitiesAction()
=======
    def updateIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateIpRangeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks for hybrid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def logOut(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogOutAction.class) Closure c) {
        def a = new org.zstack.sdk.LogOutAction()
        
=======
    def recoverVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverVmInstanceAction()
=======
    def attachDataVolumeToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachDataVolumeToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachDataVolumeToVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks for hybrid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def migrateVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MigrateVmAction.class) Closure c) {
        def a = new org.zstack.sdk.MigrateVmAction()
=======
    def querySharedResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySharedResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySharedResourceAction()
>>>>>>> update sdks for hybrid
=======
    def deleteCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteCephPrimaryStoragePoolAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def pauseVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PauseVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.PauseVmInstanceAction()
=======
    def queryAccountResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountResourceRefAction()
>>>>>>> update sdks for hybrid
=======
    def syncEcsSecurityGroupFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def powerOffBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOffBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerOffBaremetalHostAction()
=======
    def recoveryVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def checkIpAvailability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckIpAvailabilityAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckIpAvailabilityAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def powerOnBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOnBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerOnBaremetalHostAction()
=======
    def removeMonFromCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def powerResetBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerResetBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerResetBaremetalHostAction()
=======
    def syncEcsVSwitchFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVSwitchFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVSwitchFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def updateGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateGlobalConfigAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def prometheusQueryLabelValues(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryLabelValuesAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryLabelValuesAction()
=======
    def queryLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerAction()
>>>>>>> update sdks for hybrid
=======
    def queryZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryZoneAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def prometheusQueryMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryMetadataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryMetadataAction()
=======
    def queryEcsVpcFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVpcFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVpcFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def createResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateResourcePriceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachPolicyToUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPolicyToUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def prometheusQueryPassThrough(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryPassThroughAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryPassThroughAction()
=======
    def destroyVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DestroyVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DestroyVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def getPrimaryStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageTypesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def prometheusQueryVmMonitoringData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryVmMonitoringDataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryVmMonitoringDataAction()
=======
    def createVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVniRangeAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePortForwardingRuleAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getOssBucketNameFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOssBucketNameFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetOssBucketNameFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def provisionBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ProvisionBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ProvisionBaremetalHostAction()
=======
    def removeDnsFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveDnsFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveDnsFromL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def changePortForwardingRuleState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePortForwardingRuleStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePortForwardingRuleStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountAction()
=======
    def createRootVolumeTemplateFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
=======
    def attachBackupStorageToZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachBackupStorageToZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachBackupStorageToZoneAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryAccountResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountResourceRefAction()
=======
    def detachPrimaryStorageFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPrimaryStorageFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPrimaryStorageFromClusterAction()
>>>>>>> update sdks for hybrid
=======
    def queryGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGlobalConfigAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAliyunKeySecretAction()
=======
    def revertVolumeFromSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevertVolumeFromSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.RevertVolumeFromSnapshotAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryApplianceVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryApplianceVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryApplianceVmAction()
=======
    def queryCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def queryAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBackupStorageAction()
=======
    def deleteVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVniRangeAction()
>>>>>>> update sdks for hybrid
=======
    def detachPrimaryStorageFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPrimaryStorageFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPrimaryStorageFromClusterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryBaremetalChessis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalChessisAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalChessisAction()
=======
    def createEcsImageFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsImageFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsImageFromLocalImageAction()
>>>>>>> update sdks for hybrid
=======
    def addMonToCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalHostCfgAction()
=======
    def queryVirtualRouterVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterVmAction()
>>>>>>> update sdks for hybrid
=======
    def detachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachAliyunKeyAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalPxeServerAction()
=======
    def queryAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountAction()
>>>>>>> update sdks for hybrid
=======
    def queryPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPortForwardingRuleAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephBackupStorageAction()
=======
    def rebootEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootEcsInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def attachPoliciesToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPoliciesToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPoliciesToUserAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStorageAction()
=======
    def queryLocalStorageResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLocalStorageResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLocalStorageResourceRefAction()
>>>>>>> update sdks for hybrid
=======
    def reclaimSpaceFromImageStore(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReclaimSpaceFromImageStoreAction.class) Closure c) {
        def a = new org.zstack.sdk.ReclaimSpaceFromImageStoreAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStoragePoolAction()
=======
    def queryVCenterCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterClusterAction()
>>>>>>> update sdks for hybrid
=======
    def createOssBucketRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateOssBucketRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateOssBucketRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryClusterAction()
=======
    def getVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmSshKeyAction()
>>>>>>> update sdks for hybrid
=======
    def getBackupStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageTypesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryConnectionAccessPointFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConnectionAccessPointFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConnectionAccessPointFromLocalAction()
=======
    def createL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def changeInstanceOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConsoleProxyAgentAction()
=======
    def addMonToCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def addFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryDataCenterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDataCenterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDataCenterFromLocalAction()
=======
    def addOssFileBucketName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddOssFileBucketNameAction.class) Closure c) {
        def a = new org.zstack.sdk.AddOssFileBucketNameAction()
>>>>>>> update sdks for hybrid
=======
    def deletePolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePolicyAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDiskOfferingAction()
=======
    def createPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePolicyAction()
>>>>>>> update sdks for hybrid
=======
    def getCandidateIsoForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateIsoForAttachingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateIsoForAttachingVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryEcsImageFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsImageFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsImageFromLocalAction()
=======
    def isReadyToGo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsReadyToGoAction.class) Closure c) {
        def a = new org.zstack.sdk.IsReadyToGoAction()
>>>>>>> update sdks for hybrid
=======
    def reconnectImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectImageStoreBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryEcsInstanceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsInstanceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsInstanceFromLocalAction()
=======
    def updateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def getFreeIpOfL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def queryEcsSecurityGroupFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction()
=======
    def getPrimaryStorageAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryEcsSecurityGroupRuleFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction()
=======
    def deleteEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEipAction()
>>>>>>> update sdks for hybrid
=======
    def createIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateIPsecConnectionAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryEcsVSwitchFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVSwitchFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVSwitchFromLocalAction()
=======
    def deleteUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserAction()
>>>>>>> update sdks for hybrid
=======
    def deleteRouterInterfaceLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryEcsVpcFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVpcFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVpcFromLocalAction()
=======
    def deleteEcsSecurityGroupInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction()
>>>>>>> update sdks for hybrid
=======
    def queryDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDiskOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEipAction()
=======
    def startEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartEcsInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def addSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSftpBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorBackupStorageAction()
=======
    def detachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachOssBucketToEcsDataCenterAction()
>>>>>>> update sdks for hybrid
=======
    def queryIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIpRangeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorPrimaryStorageAction()
=======
    def addSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSecurityGroupRuleAction()
>>>>>>> update sdks for hybrid
=======
    def getNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNicQosAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGCJobAction()
=======
    def queryFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def deletePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePortForwardingRuleAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGlobalConfigAction()
=======
    def createEcsInstanceFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsInstanceFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsInstanceFromLocalImageAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmInstanceHaLevelAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHostAction()
=======
    def createDataVolumeTemplateFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsSecurityGroupInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryHybridEipFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHybridEipFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHybridEipFromLocalAction()
=======
    def queryDataCenterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDataCenterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDataCenterFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def getVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmQgaAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryIPSecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIPSecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIPSecConnectionAction()
=======
    def queryEcsImageFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsImageFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsImageFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def queryVCenterDatacenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterDatacenterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterDatacenterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryIdentityZoneFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIdentityZoneFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIdentityZoneFromLocalAction()
=======
    def getEcsInstanceVncUrl(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEcsInstanceVncUrlAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEcsInstanceVncUrlAction()
>>>>>>> update sdks for hybrid
=======
    def removeVmNicFromLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVmNicFromLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveVmNicFromLoadBalancerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageAction()
=======
    def addSharedMountPointPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSharedMountPointPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSharedMountPointPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def queryInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryInstanceOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageStoreBackupStorageAction()
=======
    def queryOssBucketFileName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryOssBucketFileNameAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryOssBucketFileNameAction()
>>>>>>> update sdks for hybrid
=======
    def addLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLdapServerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryInstanceOfferingAction()
=======
    def syncRouteEntryFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouteEntryFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouteEntryFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def createRouterInterfacePairRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouterInterfacePairRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouterInterfacePairRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIpRangeAction()
=======
    def createL2NoVlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2NoVlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2NoVlanNetworkAction()
>>>>>>> update sdks for hybrid
=======
    def changeVmPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVmPasswordAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2NetworkAction()
=======
    def changeImageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeImageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeImageStateAction()
>>>>>>> update sdks for hybrid
=======
    def reconnectBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VlanNetworkAction()
=======
    def changePrimaryStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePrimaryStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePrimaryStorageStateAction()
>>>>>>> update sdks for hybrid
=======
    def querySharedResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySharedResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySharedResourceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkAction()
=======
    def querySecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupAction()
>>>>>>> update sdks for hybrid
=======
    def deleteRouterInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkPoolAction()
=======
    def queryGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGCJobAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVCenterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL3NetworkAction()
=======
    def queryVCenterBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def getVmMigrationCandidateHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmMigrationCandidateHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmMigrationCandidateHostsAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapBindingAction()
=======
    def queryInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryInstanceOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def getVmCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmCapabilitiesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapServerAction()
=======
    def addVmNicToLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToLoadBalancerAction()
>>>>>>> update sdks for hybrid
=======
    def deleteZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteZoneAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerAction()
=======
    def updateBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def addFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerListenerAction()
=======
    def getVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmInstanceHaLevelAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeSnapshotAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryLocalStorageResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLocalStorageResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLocalStorageResourceRefAction()
=======
    def createVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVipAction()
>>>>>>> update sdks for hybrid
=======
    def querySystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySystemTagAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryManagementNode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryManagementNodeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryManagementNodeAction()
=======
    def deleteEcsImageRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def detachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachOssBucketToEcsDataCenterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryNetworkServiceL3NetworkRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction()
=======
    def changePortForwardingRuleState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePortForwardingRuleStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePortForwardingRuleStateAction()
>>>>>>> update sdks for hybrid
=======
    def addUserToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddUserToGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddUserToGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryNetworkServiceProvider(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceProviderAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceProviderAction()
=======
    def pauseVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PauseVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.PauseVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def addIpRangeByNetworkCidr(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeByNetworkCidrAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeByNetworkCidrAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryNotification(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationAction()
=======
    def deleteAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAliyunKeySecretAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def queryNotificationSubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationSubscriptionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationSubscriptionAction()
=======
    def getVmMigrationCandidateHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmMigrationCandidateHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmMigrationCandidateHostsAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def queryOssBucketFileName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryOssBucketFileNameAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryOssBucketFileNameAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPolicyAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPortForwardingRuleAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryQuotaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryResourcePriceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryRouteEntryFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryRouteEntryFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryRouteEntryFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryRouterInterfaceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryRouterInterfaceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryRouterInterfaceFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySchedulerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def querySecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def querySecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupRuleAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def querySftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySftpBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def querySharedResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySharedResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySharedResourceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def querySystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySystemTagAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserTagAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVCenterBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVCenterCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterClusterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVCenterDatacenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterDatacenterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterDatacenterAction()
=======
    def syncRouterInterfaceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouterInterfaceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouterInterfaceFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def queryVCenterPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterPrimaryStorageAction()
=======
    def queryNotification(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def queryVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVipAction()
=======
    def addSimulatorHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorHostAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def validateSession(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateSessionAction.class) Closure c) {
        def a = new org.zstack.sdk.ValidateSessionAction()
        
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVirtualBorderRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction()
=======
    def addIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIdentityZoneFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsVpcInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcInLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVirtualRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterFromLocalAction()
=======
    def getIpAddressCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIpAddressCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIpAddressCapacityAction()
>>>>>>> update sdks for hybrid
=======
    def queryLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapServerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterOfferingAction()
=======
    def updateEcsInstanceVncPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEcsInstanceVncPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEcsInstanceVncPasswordAction()
>>>>>>> update sdks for hybrid
=======
    def shareResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ShareResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.ShareResourceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVirtualRouterVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterVmAction()
=======
    def updateVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
=======
    def createRootVolumeTemplateFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmInstanceAction()
=======
    def getHostAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHostAllocatorStrategiesAction()
>>>>>>> update sdks for hybrid
=======
    def getCandidateVmNicForSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicAction()
=======
    def attachBackupStorageToZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachBackupStorageToZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachBackupStorageToZoneAction()
>>>>>>> update sdks for hybrid
=======
    def changeResourceOwner(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeResourceOwnerAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeResourceOwnerAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVmNicInSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicInSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicInSecurityGroupAction()
=======
    def addUserToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddUserToGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddUserToGroupAction()
>>>>>>> update sdks for hybrid
=======
    def updateNotificationsStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateNotificationsStatusAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateNotificationsStatusAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVniRangeAction()
=======
    def queryGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGlobalConfigAction()
>>>>>>> update sdks for hybrid
=======
    def setVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmBootOrderAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeAction()
=======
    def changeClusterState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeClusterStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeClusterStateAction()
>>>>>>> update sdks for hybrid
=======
    def reconnectVirtualRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectVirtualRouterAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectVirtualRouterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotAction()
=======
    def getPrimaryStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageTypesAction()
>>>>>>> update sdks for hybrid
=======
    def addSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSecurityGroupRuleAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryVolumeSnapshotTree(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotTreeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotTreeAction()
=======
    def queryHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHostAction()
>>>>>>> update sdks for hybrid
=======
    def createVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVniRangeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryWebhookAction()
=======
    def updateInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateInstanceOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsVSwitchInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchInLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def queryZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryZoneAction()
=======
    def updateQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateQuotaAction()
>>>>>>> update sdks for hybrid
=======
    def setVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVolumeQosAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def rebootEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootEcsInstanceAction()
=======
    def cleanUpImageCacheOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def updateInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateInstanceOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def rebootVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootVmInstanceAction()
=======
    def queryL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def queryPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def reclaimSpaceFromImageStore(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReclaimSpaceFromImageStoreAction.class) Closure c) {
        def a = new org.zstack.sdk.ReclaimSpaceFromImageStoreAction()
=======
    def detachPolicyFromUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserGroupAction()
>>>>>>> update sdks for hybrid
=======
    def querySecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def reconnectBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectBackupStorageAction()
=======
    def changeInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def queryEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def reconnectConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectConsoleProxyAgentAction()
=======
    def setVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmBootOrderAction()
>>>>>>> update sdks for hybrid
=======
    def createPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePolicyAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def reconnectHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectHostAction()
=======
    def deleteExportedImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteExportedImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteExportedImageFromBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def syncEcsInstanceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsInstanceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsInstanceFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def reconnectImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectImageStoreBackupStorageAction()
=======
    def recoverDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverDataVolumeAction()
>>>>>>> update sdks for hybrid
=======
    def queryManagementNode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryManagementNodeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryManagementNodeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def reconnectPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectPrimaryStorageAction()
=======
    def getVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmQgaAction()
>>>>>>> update sdks for hybrid
=======
    def createUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def reconnectVirtualRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectVirtualRouterAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectVirtualRouterAction()
=======
    def setVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmConsolePasswordAction()
>>>>>>> update sdks for hybrid
=======
    def changeImageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeImageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeImageStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def recoverDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverDataVolumeAction()
=======
    def queryVmNicInSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicInSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicInSecurityGroupAction()
>>>>>>> update sdks for hybrid
=======
    def deleteExportedImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteExportedImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteExportedImageFromBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def recoverImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverImageAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverImageAction()
=======
    def updateVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualRouterOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def deleteDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def recoverVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverVmInstanceAction()
=======
    def updateGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateGlobalConfigAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVirtualBorderRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualBorderRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualBorderRouterLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def recoveryVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction()
=======
    def updatePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def cloneVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CloneVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CloneVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def refreshLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RefreshLoadBalancerAction()
=======
    def stopVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def deleteAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAliyunKeySecretAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def reimageVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReimageVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ReimageVmInstanceAction()
=======
    def deleteVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmInstanceHaLevelAction()
>>>>>>> update sdks for hybrid
=======
    def deleteDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDiskOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def reloadLicense(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReloadLicenseAction.class) Closure c) {
        def a = new org.zstack.sdk.ReloadLicenseAction()
=======
    def getL2NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL2NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL2NetworkTypesAction()
>>>>>>> update sdks for hybrid
=======
    def queryUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def removeDnsFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveDnsFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveDnsFromL3NetworkAction()
=======
    def createResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateResourcePriceAction()
>>>>>>> update sdks for hybrid
=======
    def setVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmInstanceHaLevelAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def removeMonFromCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephBackupStorageAction()
=======
    def attachPolicyToUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPolicyToUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserGroupAction()
>>>>>>> update sdks for hybrid
=======
    def addCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def removeMonFromCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction()
=======
    def updateSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSecurityGroupAction()
>>>>>>> update sdks for hybrid
=======
    def addOssFileBucketName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddOssFileBucketNameAction.class) Closure c) {
        def a = new org.zstack.sdk.AddOssFileBucketNameAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def removeMonFromFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction()
=======
    def prometheusQueryPassThrough(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryPassThroughAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryPassThroughAction()
>>>>>>> update sdks for hybrid
=======
    def deleteIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIPsecConnectionAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def removeMonFromFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction()
=======
    def addDnsToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDnsToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDnsToL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def updateSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSecurityGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def removeUserFromGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveUserFromGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveUserFromGroupAction()
=======
    def addIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def removeVmNicFromLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVmNicFromLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveVmNicFromLoadBalancerAction()
=======
    def reconnectVirtualRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectVirtualRouterAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectVirtualRouterAction()
>>>>>>> update sdks for hybrid
=======
    def getBackupStorageForCreatingImageFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def requestConsoleAccess(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RequestConsoleAccessAction.class) Closure c) {
        def a = new org.zstack.sdk.RequestConsoleAccessAction()
=======
    def queryL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkPoolAction()
>>>>>>> update sdks for hybrid
=======
    def removeMonFromFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def resumeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResumeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ResumeVmInstanceAction()
=======
    def expungeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def addNfsPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddNfsPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddNfsPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def revertVolumeFromSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevertVolumeFromSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.RevertVolumeFromSnapshotAction()
=======
    def deleteEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def terminateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def revokeResourceSharing(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevokeResourceSharingAction.class) Closure c) {
        def a = new org.zstack.sdk.RevokeResourceSharingAction()
=======
    def updateCephPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephPrimaryStorageMonAction()
>>>>>>> update sdks for hybrid
=======
    def deleteOssFileBucketNameInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssFileBucketNameInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssFileBucketNameInLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetImageQgaAction()
=======
    def updateAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateAccountAction()
>>>>>>> update sdks for hybrid
=======
    def detachBackupStorageFromZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachBackupStorageFromZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachBackupStorageFromZoneAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetNicQosAction()
=======
    def deleteScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSchedulerAction()
>>>>>>> update sdks for hybrid
=======
    def rebootVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmBootOrderAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logInByAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByAccountAction()
        
>>>>>>> update sdks for hybrid
=======
    def queryLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerListenerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmConsolePasswordAction()
=======
    def updateImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageAction()
>>>>>>> update sdks for hybrid
=======
    def queryEcsSecurityGroupRuleFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmHostnameAction()
=======
    def deleteUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserGroupAction()
>>>>>>> update sdks for hybrid
=======
    def deleteNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNicQosAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmInstanceHaLevelAction()
=======
    def checkApiPermission(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckApiPermissionAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckApiPermissionAction()
>>>>>>> update sdks for hybrid
=======
    def changeZoneState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeZoneStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeZoneStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmQgaAction()
=======
    def queryNotificationSubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationSubscriptionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationSubscriptionAction()
>>>>>>> update sdks for hybrid
=======
    def updateKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateKVMHostAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmSshKeyAction()
=======
    def deleteHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHostAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteHostAction()
>>>>>>> update sdks for hybrid
=======
    def deleteIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIpRangeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmStaticIpAction()
=======
    def queryResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryResourcePriceAction()
>>>>>>> update sdks for hybrid
=======
    def syncImageSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncImageSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncImageSizeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def setVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVolumeQosAction()
=======
    def stopEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopEcsInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def createUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserTagAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def shareResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ShareResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.ShareResourceAction()
=======
    def createAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateAccountAction()
>>>>>>> update sdks for hybrid
=======
    def addIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIdentityZoneFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def startBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.StartBaremetalPxeServerAction()
=======
    def kvmRunShell(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.KvmRunShellAction.class) Closure c) {
        def a = new org.zstack.sdk.KvmRunShellAction()
>>>>>>> update sdks for hybrid
=======
    def queryVirtualRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def startEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartEcsInstanceAction()
=======
    def deleteLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerAction()
>>>>>>> update sdks for hybrid
=======
    def updateCephBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephBackupStorageMonAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def startVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartVmInstanceAction()
=======
    def reconnectConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectConsoleProxyAgentAction()
>>>>>>> update sdks for hybrid
=======
    def resumeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResumeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ResumeVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def stopBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.StopBaremetalPxeServerAction()
=======
    def getVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsolePasswordAction()
>>>>>>> update sdks for hybrid
=======
    def createZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateZoneAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def stopEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopEcsInstanceAction()
=======
    def addIpRangeByNetworkCidr(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeByNetworkCidrAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeByNetworkCidrAction()
>>>>>>> update sdks for hybrid
=======
    def queryApplianceVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryApplianceVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryApplianceVmAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def stopVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopVmInstanceAction()
=======
    def updateImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageStoreBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def attachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachOssBucketToEcsDataCenterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncEcsImageFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsImageFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsImageFromRemoteAction()
=======
    def setVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmHostnameAction()
>>>>>>> update sdks for hybrid
=======
    def addAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.AddAliyunKeySecretAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncEcsInstanceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsInstanceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsInstanceFromRemoteAction()
=======
    def updateFusionstorPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction()
>>>>>>> update sdks for hybrid
=======
    def getIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIdentityZoneFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncEcsSecurityGroupFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction()
=======
    def attachPrimaryStorageToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPrimaryStorageToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPrimaryStorageToClusterAction()
>>>>>>> update sdks for hybrid
=======
    def reimageVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReimageVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ReimageVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncEcsVSwitchFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVSwitchFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVSwitchFromRemoteAction()
=======
    def revokeResourceSharing(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevokeResourceSharingAction.class) Closure c) {
        def a = new org.zstack.sdk.RevokeResourceSharingAction()
>>>>>>> update sdks for hybrid
=======
    def getInterdependentL3NetworksImages(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetInterdependentL3NetworksImagesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetInterdependentL3NetworksImagesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncEcsVpcFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVpcFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVpcFromRemoteAction()
=======
    def reconnectPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectPrimaryStorageAction()
>>>>>>> update sdks for hybrid
=======
    def updateRouteInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateRouteInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateRouteInterfaceRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncImageSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncImageSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncImageSizeAction()
=======
    def deleteAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAccountAction()
>>>>>>> update sdks for hybrid
=======
    def queryUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserTagAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncPrimaryStorageCapacityAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def getVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVersionAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVersionAction()
        
>>>>>>> update sdks for hybrid
=======
    def getVmStartingCandidateClustersHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmStartingCandidateClustersHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmStartingCandidateClustersHostsAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncRouteEntryFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouteEntryFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouteEntryFromRemoteAction()
=======
    def deletePolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePolicyAction()
>>>>>>> update sdks for hybrid
=======
    def deleteTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteTagAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteTagAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncRouterInterfaceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouterInterfaceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouterInterfaceFromRemoteAction()
=======
    def changeSecurityGroupState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSecurityGroupStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSecurityGroupStateAction()
>>>>>>> update sdks for hybrid
=======
    def updateFusionstorBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorBackupStorageMonAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncVirtualBorderRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction()
=======
    def getCandidateZonesClustersHostsForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction()
>>>>>>> update sdks for hybrid
=======
    def attachPrimaryStorageToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPrimaryStorageToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPrimaryStorageToClusterAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncVirtualRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualRouterFromRemoteAction()
=======
    def getCandidateVmNicsForLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction()
>>>>>>> update sdks for hybrid
=======
    def deleteOssBucketFileRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssBucketFileRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssBucketFileRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def syncVolumeSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVolumeSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVolumeSizeAction()
=======
    def deleteSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupAction()
>>>>>>> update sdks for hybrid
=======
    def syncEcsVSwitchFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVSwitchFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVSwitchFromRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def terminateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction()
=======
    def queryRouteEntryFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryRouteEntryFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryRouteEntryFromLocalAction()
>>>>>>> update sdks for hybrid
=======
    def addMonToFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def triggerGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TriggerGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.TriggerGCJobAction()
=======
    def queryPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPortForwardingRuleAction()
>>>>>>> update sdks for hybrid
=======
    def removeUserFromGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveUserFromGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveUserFromGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateAccountAction()
=======
    def querySecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupRuleAction()
>>>>>>> update sdks for hybrid
=======
    def getVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmSshKeyAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBackupStorageAction()
=======
    def addImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageStoreBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def startEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartEcsInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateBaremetalChessis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalChessisAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBaremetalChessisAction()
=======
    def addMonToFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorBackupStorageAction()
>>>>>>> update sdks for hybrid
=======
    def updateEcsInstanceVncPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEcsInstanceVncPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEcsInstanceVncPasswordAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBaremetalHostCfgAction()
=======
    def updateKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateKVMHostAction()
>>>>>>> update sdks for hybrid
=======
    def updateVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualRouterOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBaremetalPxeServerAction()
=======
    def deleteVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmHostnameAction()
>>>>>>> update sdks for hybrid
=======
    def getVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmBootOrderAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateCephBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephBackupStorageMonAction()
=======
    def createVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVirtualRouterOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def attachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachAliyunKeyAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateCephPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephPrimaryStorageMonAction()
=======
    def deleteVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmConsolePasswordAction()
>>>>>>> update sdks for hybrid
=======
    def updateHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateHostAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateClusterAction()
=======
    def queryCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryClusterAction()
>>>>>>> update sdks for hybrid
=======
    def kvmRunShell(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.KvmRunShellAction.class) Closure c) {
        def a = new org.zstack.sdk.KvmRunShellAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateDiskOfferingAction()
=======
    def updateSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSystemTagAction()
>>>>>>> update sdks for hybrid
=======
    def getHypervisorTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHypervisorTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHypervisorTypesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateEcsInstanceVncPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEcsInstanceVncPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEcsInstanceVncPasswordAction()
=======
    def attachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachOssBucketToEcsDataCenterAction()
>>>>>>> update sdks for hybrid
=======
    def deleteUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEipAction()
=======
    def getEipAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEipAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEipAttachableVmNicsAction()
>>>>>>> update sdks for hybrid
=======
    def changePrimaryStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePrimaryStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePrimaryStorageStateAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateFusionstorBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorBackupStorageMonAction()
=======
    def createUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserGroupAction()
>>>>>>> update sdks for hybrid
=======
    def createL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkPoolAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateFusionstorPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction()
=======
    def syncImageSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncImageSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncImageSizeAction()
>>>>>>> update sdks for hybrid
=======
    def updateEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEipAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateGlobalConfigAction()
=======
    def attachSecurityGroupToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachSecurityGroupToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachSecurityGroupToL3NetworkAction()
>>>>>>> update sdks for hybrid
=======
    def getVmAttachableL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableL3NetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateHostAction()
=======
    def getLocalStorageHostDiskCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLocalStorageHostDiskCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLocalStorageHostDiskCapacityAction()
>>>>>>> update sdks for hybrid
=======
    def addVmNicToSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToSecurityGroupAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageAction()
=======
    def updatePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePortForwardingRuleAction()
>>>>>>> update sdks for hybrid
=======
    def queryL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VlanNetworkAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageStoreBackupStorageAction()
=======
    def queryEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEipAction()
>>>>>>> update sdks for hybrid
=======
    def prometheusQueryPassThrough(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryPassThroughAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryPassThroughAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateInstanceOfferingAction()
=======
    def queryUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserAction()
>>>>>>> update sdks for hybrid
=======
    def recoverDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverDataVolumeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateIpRangeAction()
=======
    def syncPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncPrimaryStorageCapacityAction()
>>>>>>> update sdks for hybrid
=======
    def updateVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVmInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateKVMHostAction()
=======
    def deleteVmNicFromSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmNicFromSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmNicFromSecurityGroupAction()
>>>>>>> update sdks for hybrid
=======
    def queryVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVniRangeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL2NetworkAction()
=======
    def getPrimaryStorageAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction()
>>>>>>> update sdks for hybrid
=======
    def queryAccountResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountResourceRefAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL3NetworkAction()
=======
    def getBackupStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageCapacityAction()
>>>>>>> update sdks for hybrid
=======
    def deleteVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmSshKeyAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateLdapServerAction()
=======
    def setVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVolumeQosAction()
>>>>>>> update sdks for hybrid
=======
    def setNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetNicQosAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateNotificationsStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateNotificationsStatusAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateNotificationsStatusAction()
=======
    def queryUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserTagAction()
>>>>>>> update sdks for hybrid
=======
    def deleteEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updatePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePortForwardingRuleAction()
=======
    def deleteEcsImageLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageLocalAction()
>>>>>>> update sdks for hybrid
=======
    def updateImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageStoreBackupStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updatePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePrimaryStorageAction()
=======
    def queryNetworkServiceL3NetworkRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction()
>>>>>>> update sdks for hybrid
=======
    def getL2NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL2NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL2NetworkTypesAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateQuotaAction()
=======
    def addDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDataCenterFromRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def syncVolumeSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVolumeSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVolumeSizeAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateRouteInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateRouteInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateRouteInterfaceRemoteAction()
=======
    def querySystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySystemTagAction()
>>>>>>> update sdks for hybrid
=======
    def createEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVSwitchRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSchedulerAction()
=======
    def queryIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIpRangeAction()
>>>>>>> update sdks for hybrid
=======
    def queryL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkPoolAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSecurityGroupAction()
=======
    def deleteSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupRuleAction()
>>>>>>> update sdks for hybrid
=======
    def getVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmHostnameAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSftpBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def validateSession(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateSessionAction.class) Closure c) {
        def a = new org.zstack.sdk.ValidateSessionAction()
        
>>>>>>> update sdks for hybrid
=======
    def addSimulatorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSystemTagAction()
=======
    def queryUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserGroupAction()
>>>>>>> update sdks for hybrid
=======
    def getVmConsoleAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsoleAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsoleAddressAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserAction()
=======
    def createVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVmInstanceAction()
>>>>>>> update sdks for hybrid
=======
    def addSimulatorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserGroupAction()
=======
    def queryConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConsoleProxyAgentAction()
>>>>>>> update sdks for hybrid
=======
    def updatePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVipAction()
=======
    def detachBackupStorageFromZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachBackupStorageFromZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachBackupStorageFromZoneAction()
>>>>>>> update sdks for hybrid
=======
    def queryFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction()
=======
    def deleteEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcRemoteAction()
>>>>>>> update sdks for hybrid
=======
    def queryHybridEipFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHybridEipFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHybridEipFromLocalAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualRouterOfferingAction()
=======
    def createDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDiskOfferingAction()
>>>>>>> update sdks for hybrid
=======
    def createVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVirtualRouterOfferingAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVmInstanceAction()
=======
    def detachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachEipAction()
>>>>>>> update sdks for hybrid
=======
    def setVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmStaticIpAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeAction()
=======
    def recoverImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverImageAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverImageAction()
>>>>>>> update sdks for hybrid
=======
    def getL3NetworkDhcpIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkDhcpIpAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkDhcpIpAddressAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeSnapshotAction()
=======
    def changeInstanceOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingStateAction()
>>>>>>> update sdks for hybrid
=======
    def addCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStorageAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateWebhookAction()
=======
    def removeVmNicFromLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVmNicFromLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveVmNicFromLoadBalancerAction()
>>>>>>> update sdks for hybrid
=======
    def deleteRouteEntryRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouteEntryRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouteEntryRemoteAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def updateZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateZoneAction()
=======
    def getBackupStorageForCreatingImageFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
=======
    def triggerGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TriggerGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.TriggerGCJobAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
<<<<<<< HEAD
    def validateSession(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateSessionAction.class) Closure c) {
        def a = new org.zstack.sdk.ValidateSessionAction()
        
=======
    def createEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRemoteAction()
=======
    def rebootEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootEcsInstanceAction()
>>>>>>> update sdks
        a.sessionId = Test.currentEnvSpec?.session?.uuid
>>>>>>> update sdks for hybrid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


}
