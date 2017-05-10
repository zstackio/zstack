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
        def addAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.AddAliyunKeySecretAction()
=======
        def getVmConsoleAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsoleAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsoleAddressAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephBackupStorageAction()
=======
    def changeEipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeEipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeEipStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStorageAction()
=======
    def expungeDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeDataVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStoragePoolAction()
=======
    def queryVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVniRangeAction()
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
    def addConnectionAccessPointFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddConnectionAccessPointFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddConnectionAccessPointFromRemoteAction()
=======
    def queryVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVipAction()
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
    def addDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDataCenterFromRemoteAction()
=======
    def startVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addDnsToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDnsToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDnsToL3NetworkAction()
=======
    def getVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmHostnameAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorBackupStorageAction()
=======
    def getBackupStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageTypesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorPrimaryStorageAction()
=======
    def getL3NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkTypesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIdentityZoneFromRemoteAction()
=======
    def setVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmQgaAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageAction()
=======
    def createRootVolumeTemplateFromRootVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageStoreBackupStorageAction()
=======
    def queryScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySchedulerAction()
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
    def addIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeAction()
=======
    def getPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageCapacityAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addIpRangeByNetworkCidr(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeByNetworkCidrAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeByNetworkCidrAction()
=======
    def detachPoliciesFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPoliciesFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPoliciesFromUserAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddKVMHostAction()
=======
    def updateVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVipAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLdapServerAction()
=======
    def createSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSystemTagAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addLocalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLocalPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLocalPrimaryStorageAction()
=======
    def queryPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPrimaryStorageAction()
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
    def addMonToCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephBackupStorageAction()
=======
    def shareResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ShareResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.ShareResourceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addMonToCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephPrimaryStorageAction()
=======
    def syncEcsImageFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsImageFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsImageFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addMonToFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorBackupStorageAction()
=======
    def getTaskProgress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTaskProgressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetTaskProgressAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addMonToFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logInByUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByUserAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByUserAction()
        
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
    def addNfsPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddNfsPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddNfsPrimaryStorageAction()
=======
    def localStorageGetVolumeMigratableHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addOssFileBucketName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddOssFileBucketNameAction.class) Closure c) {
        def a = new org.zstack.sdk.AddOssFileBucketNameAction()
=======
    def getNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNicQosAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSecurityGroupRuleAction()
=======
    def queryEcsSecurityGroupRuleFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction()
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
    def addSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSftpBackupStorageAction()
=======
    def queryLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerListenerAction()
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
    def addSharedMountPointPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSharedMountPointPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSharedMountPointPrimaryStorageAction()
=======
    def createPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePortForwardingRuleAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addSimulatorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorBackupStorageAction()
=======
    def createInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateInstanceOfferingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addSimulatorHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorHostAction()
=======
    def createSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSecurityGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addSimulatorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorPrimaryStorageAction()
=======
    def detachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachAliyunKeyAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addUserToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddUserToGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddUserToGroupAction()
=======
    def checkIpAvailability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckIpAvailabilityAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckIpAvailabilityAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVCenterAction()
=======
    def changeVolumeState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVolumeStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVolumeStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addVmNicToLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToLoadBalancerAction()
=======
    def createRouterInterfacePairRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouterInterfacePairRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouterInterfacePairRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def addVmNicToSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToSecurityGroupAction()
=======
    def refreshLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RefreshLoadBalancerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachAliyunKeyAction()
=======
    def queryVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterAction()
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
    def attachBackupStorageToZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachBackupStorageToZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachBackupStorageToZoneAction()
=======
    def reloadLicense(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReloadLicenseAction.class) Closure c) {
        def a = new org.zstack.sdk.ReloadLicenseAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachDataVolumeToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachDataVolumeToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachDataVolumeToVmAction()
=======
    def queryFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachEipAction()
=======
    def detachIsoFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachIsoFromVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachIsoFromVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachIsoToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachIsoToVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachIsoToVmInstanceAction()
=======
    def detachSecurityGroupFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachL2NetworkToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL2NetworkToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL2NetworkToClusterAction()
=======
    def attachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPortForwardingRuleAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachL3NetworkToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL3NetworkToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL3NetworkToVmAction()
=======
    def attachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachEipAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachNetworkServiceToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachNetworkServiceToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachNetworkServiceToL3NetworkAction()
=======
    def attachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachAliyunKeyAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachOssBucketToEcsDataCenterAction()
=======
    def deleteEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachPoliciesToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPoliciesToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPoliciesToUserAction()
=======
    def addAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.AddAliyunKeySecretAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachPolicyToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPolicyToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserAction()
=======
    def updateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachPolicyToUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPolicyToUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserGroupAction()
=======
    def changeVmPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVmPasswordAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPortForwardingRuleAction()
=======
    def createDataVolumeFromVolumeTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachPrimaryStorageToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPrimaryStorageToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPrimaryStorageToClusterAction()
=======
    def deleteL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL2NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def attachSecurityGroupToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachSecurityGroupToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachSecurityGroupToL3NetworkAction()
=======
    def createL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkPoolAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def calculateAccountSpending(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateAccountSpendingAction.class) Closure c) {
        def a = new org.zstack.sdk.CalculateAccountSpendingAction()
=======
    def queryEcsInstanceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsInstanceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsInstanceFromLocalAction()
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
    def changeBackupStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeBackupStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeBackupStorageStateAction()
=======
    def updateUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeClusterState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeClusterStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeClusterStateAction()
=======
    def getL3NetworkDhcpIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkDhcpIpAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkDhcpIpAddressAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeDiskOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeDiskOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeDiskOfferingStateAction()
=======
    def createLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerListenerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeEipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeEipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeEipStateAction()
=======
    def addIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeHostState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeHostStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeHostStateAction()
=======
    def migrateVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MigrateVmAction.class) Closure c) {
        def a = new org.zstack.sdk.MigrateVmAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeImageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeImageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeImageStateAction()
=======
    def changeSchedulerState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSchedulerStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSchedulerStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingAction()
=======
    def getCpuMemoryCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCpuMemoryCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCpuMemoryCapacityAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeInstanceOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingStateAction()
=======
    def queryVCenterPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeL3NetworkState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeL3NetworkStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeL3NetworkStateAction()
=======
    def createEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVSwitchRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changePortForwardingRuleState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePortForwardingRuleStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePortForwardingRuleStateAction()
=======
    def createZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateZoneAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changePrimaryStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePrimaryStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePrimaryStorageStateAction()
=======
    def queryNetworkServiceProvider(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceProviderAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceProviderAction()
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
    def changeResourceOwner(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeResourceOwnerAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeResourceOwnerAction()
=======
    def updateCephBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephBackupStorageMonAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeSchedulerState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSchedulerStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSchedulerStateAction()
=======
    def getDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetDataCenterFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeSecurityGroupState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSecurityGroupStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSecurityGroupStateAction()
=======
    def deleteVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmStaticIpAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeVipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVipStateAction()
=======
    def updateL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL2NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeVmPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVmPasswordAction()
=======
    def updateLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateLdapServerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def changeVolumeState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVolumeStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVolumeStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def getCurrentTime(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCurrentTimeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCurrentTimeAction()
        
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
    def changeZoneState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeZoneStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeZoneStateAction()
=======
    def deleteAllEcsInstancesFromDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def checkApiPermission(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckApiPermissionAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckApiPermissionAction()
=======
    def queryIPSecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIPSecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIPSecConnectionAction()
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
    def checkIpAvailability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckIpAvailabilityAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckIpAvailabilityAction()
=======
    def deleteVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeQosAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def cleanInvalidLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanInvalidLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanInvalidLdapBindingAction()
=======
    def deletePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def cleanUpImageCacheOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction()
=======
    def syncEcsInstanceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsInstanceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsInstanceFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def cloneVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CloneVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CloneVmInstanceAction()
=======
    def getVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmBootOrderAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateAccountAction()
=======
    def queryCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStoragePoolAction()
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
    def createBaremetalChessis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalChessisAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateBaremetalChessisAction()
=======
    def createStartVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStartVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStartVmInstanceSchedulerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateBaremetalHostCfgAction()
=======
    def queryL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VlanNetworkAction()
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
    def createBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateBaremetalPxeServerAction()
=======
    def updateScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSchedulerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateClusterAction()
=======
    def prometheusQueryVmMonitoringData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryVmMonitoringDataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryVmMonitoringDataAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeAction()
=======
    def deleteEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createDataVolumeFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction()
=======
    def removeMonFromCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createDataVolumeFromVolumeTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction()
=======
    def createRebootVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRebootVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRebootVmInstanceSchedulerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createDataVolumeTemplateFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction()
=======
    def syncRouterInterfaceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouterInterfaceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouterInterfaceFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDiskOfferingAction()
=======
    def createUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserTagAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createEcsImageFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsImageFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsImageFromLocalImageAction()
=======
    def deleteDataCenterInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataCenterInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataCenterInLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createEcsInstanceFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsInstanceFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsInstanceFromLocalImageAction()
=======
    def deleteVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVCenterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRemoteAction()
=======
    def queryManagementNode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryManagementNodeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryManagementNodeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction()
=======
    def updateVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVSwitchRemoteAction()
=======
    def deletePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePortForwardingRuleAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVpcRemoteAction()
=======
    def updateRouteInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateRouteInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateRouteInterfaceRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEipAction()
=======
    def getOssBucketNameFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOssBucketNameFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetOssBucketNameFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateIPsecConnectionAction()
=======
    def getNetworkServiceTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNetworkServiceTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNetworkServiceTypesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateInstanceOfferingAction()
=======
    def resumeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResumeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ResumeVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createL2NoVlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2NoVlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2NoVlanNetworkAction()
=======
    def deleteEcsVSwitchInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchInLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VlanNetworkAction()
=======
    def setNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetNicQosAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkAction()
=======
    def prometheusQueryLabelValues(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryLabelValuesAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryLabelValuesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkPoolAction()
=======
    def queryQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryQuotaAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL3NetworkAction()
=======
    def reimageVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReimageVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ReimageVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLdapBindingAction()
=======
    def getFreeIpOfL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerAction()
=======
    def queryApplianceVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryApplianceVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryApplianceVmAction()
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
    def createLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerListenerAction()
=======
    def detachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPortForwardingRuleAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePolicyAction()
=======
    def syncVirtualRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualRouterFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePortForwardingRuleAction()
=======
    def updateZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateZoneAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createRebootVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRebootVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRebootVmInstanceSchedulerAction()
=======
    def getVmStartingCandidateClustersHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmStartingCandidateClustersHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmStartingCandidateClustersHostsAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateResourcePriceAction()
=======
    def queryL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createRootVolumeTemplateFromRootVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction()
=======
    def deleteVirtualBorderRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualBorderRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualBorderRouterLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createRootVolumeTemplateFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction()
=======
    def queryConnectionAccessPointFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConnectionAccessPointFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConnectionAccessPointFromLocalAction()
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
    def createRouteEntryForConnectionRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction()
=======
    def createLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLdapBindingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createRouterInterfacePairRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouterInterfacePairRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouterInterfacePairRemoteAction()
=======
    def triggerGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TriggerGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.TriggerGCJobAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSecurityGroupAction()
=======
    def createRouteEntryForConnectionRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouteEntryForConnectionRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createStartVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStartVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStartVmInstanceSchedulerAction()
=======
    def cleanInvalidLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanInvalidLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanInvalidLdapBindingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createStopVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStopVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStopVmInstanceSchedulerAction()
=======
    def syncVirtualBorderRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSystemTagAction()
=======
    def deleteEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserAction()
=======
    def deleteNotifications(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNotificationsAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNotificationsAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserGroupAction()
=======
    def rebootVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserTagAction()
=======
    def queryVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotAction()
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
    def createVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVipAction()
=======
    def getCandidateVmNicForSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVirtualRouterOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logOut(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogOutAction.class) Closure c) {
        def a = new org.zstack.sdk.LogOutAction()
        
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
    def createVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVmInstanceAction()
=======
    def getAccountQuotaUsage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAccountQuotaUsageAction.class) Closure c) {
        def a = new org.zstack.sdk.GetAccountQuotaUsageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVniRangeAction()
=======
    def deleteIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIpRangeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotAction()
=======
    def queryVCenterDatacenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterDatacenterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterDatacenterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createVolumeSnapshotScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotSchedulerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logInByLdap(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByLdapAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByLdapAction()
        
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
    def createWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateWebhookAction()
=======
    def removeUserFromGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveUserFromGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveUserFromGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def createZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateZoneAction()
=======
    def deleteLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapBindingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAccountAction()
=======
    def queryL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkAction()
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
    def deleteAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAliyunKeySecretAction()
=======
    def queryCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephBackupStorageAction()
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
    def deleteAllEcsInstancesFromDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction()
=======
    def addSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSftpBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBackupStorageAction()
=======
    def requestConsoleAccess(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RequestConsoleAccessAction.class) Closure c) {
        def a = new org.zstack.sdk.RequestConsoleAccessAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteBaremetalChessis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalChessisAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBaremetalChessisAction()
=======
    def deleteL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBaremetalHostCfgAction()
=======
    def createEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRuleRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBaremetalPxeServerAction()
=======
    def getCandidateIsoForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateIsoForAttachingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateIsoForAttachingVmAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteCephPrimaryStoragePoolAction()
=======
    def changeDiskOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeDiskOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeDiskOfferingStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteClusterAction()
=======
    def queryAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAliyunKeySecretAction()
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
    def deleteConnectionAccessPointLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteConnectionAccessPointLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteConnectionAccessPointLocalAction()
=======
    def queryVirtualRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterFromLocalAction()
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
    def deleteDataCenterInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataCenterInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataCenterInLocalAction()
=======
    def updateDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateDiskOfferingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataVolumeAction()
=======
    def queryIdentityZoneFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIdentityZoneFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIdentityZoneFromLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDiskOfferingAction()
=======
    def changeZoneState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeZoneStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeZoneStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEcsImageLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageLocalAction()
=======
    def createLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEcsImageRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageRemoteAction()
=======
    def addFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsInstanceAction()
=======
    def attachL2NetworkToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL2NetworkToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL2NetworkToClusterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEcsSecurityGroupInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction()
=======
    def deleteRouteEntryRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouteEntryRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouteEntryRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction()
=======
    def createEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVpcRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction()
=======
    def queryEcsVSwitchFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVSwitchFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVSwitchFromLocalAction()
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
    def deleteEcsVSwitchInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchInLocalAction()
=======
    def updateNotificationsStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateNotificationsStatusAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateNotificationsStatusAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchRemoteAction()
=======
    def getBackupStorageForCreatingImageFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEcsVpcInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcInLocalAction()
=======
    def changeHostState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeHostStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeHostStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcRemoteAction()
=======
    def getInterdependentL3NetworksImages(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetInterdependentL3NetworksImagesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetInterdependentL3NetworksImagesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEipAction()
=======
    def deleteTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteTagAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteTagAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteExportedImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteExportedImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteExportedImageFromBackupStorageAction()
=======
    def deleteVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmSshKeyAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteGCJobAction()
=======
    def addVmNicToSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToSecurityGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHostAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteHostAction()
=======
    def calculateAccountSpending(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateAccountSpendingAction.class) Closure c) {
        def a = new org.zstack.sdk.CalculateAccountSpendingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIPsecConnectionAction()
=======
    def deleteRouterInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteIdentityZoneInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIdentityZoneInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIdentityZoneInLocalAction()
=======
    def getLicenseCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLicenseCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseCapabilitiesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteImageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteImageAction()
=======
    def deleteVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteInstanceOfferingAction()
=======
    def deleteImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteImageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteImageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIpRangeAction()
=======
    def queryVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL2NetworkAction()
=======
    def removeMonFromFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL3NetworkAction()
=======
    def getVolumeFormat(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeFormatAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeFormatAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapBindingAction()
=======
    def setVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmSshKeyAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapServerAction()
=======
    def changeVipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVipStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerAction()
=======
    def removeMonFromFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerListenerAction()
=======
    def exportImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExportImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExportImageFromBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNicQosAction()
=======
    def deleteCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteClusterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteNotifications(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNotificationsAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNotificationsAction()
=======
    def queryNotification(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationAction()
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
    def deleteOssFileBucketNameInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssFileBucketNameInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssFileBucketNameInLocalAction()
=======
    def deleteLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerListenerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deletePolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePolicyAction()
=======
    def queryHybridEipFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHybridEipFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHybridEipFromLocalAction()
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
    def deletePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePortForwardingRuleAction()
=======
    def localStorageMigrateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageMigrateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageMigrateVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deletePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePrimaryStorageAction()
=======
    def queryVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmInstanceAction()
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
    def deleteResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteResourcePriceAction()
=======
    def updateEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEipAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteRouteEntryRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouteEntryRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouteEntryRemoteAction()
=======
    def queryVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterOfferingAction()
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
    def deleteRouterInterfaceLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceLocalAction()
=======
    def deleteZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteZoneAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteRouterInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceRemoteAction()
=======
    def updateCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateClusterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSchedulerAction()
=======
    def deleteRouterInterfaceLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupAction()
=======
    def updateL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupRuleAction()
=======
    def addSimulatorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteTagAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteTagAction()
=======
    def setImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetImageQgaAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserAction()
=======
    def createEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEipAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserGroupAction()
=======
    def setVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmStaticIpAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVCenterAction()
=======
    def addSimulatorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVipAction()
=======
    def updateIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateIpRangeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVirtualBorderRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualBorderRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualBorderRouterLocalAction()
=======
    def createVolumeSnapshotScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotSchedulerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVirtualRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualRouterLocalAction()
=======
    def detachL3NetworkFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL3NetworkFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL3NetworkFromVmAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmConsolePasswordAction()
=======
    def changeL3NetworkState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeL3NetworkStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeL3NetworkStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmHostnameAction()
=======
    def detachDataVolumeFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachDataVolumeFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachDataVolumeFromVmAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmInstanceHaLevelAction()
=======
    def createL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VlanNetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVmNicFromSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmNicFromSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmNicFromSecurityGroupAction()
=======
    def queryDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDiskOfferingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmSshKeyAction()
=======
    def deleteDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmStaticIpAction()
=======
    def addLocalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLocalPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLocalPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVniRangeAction()
=======
    def createUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeQosAction()
=======
    def detachPolicyFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeSnapshotAction()
=======
    def deleteIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIPsecConnectionAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteWebhookAction()
=======
    def prometheusQueryMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryMetadataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryMetadataAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def deleteZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteZoneAction()
=======
    def queryVolumeSnapshotTree(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotTreeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotTreeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def destroyVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DestroyVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DestroyVmInstanceAction()
=======
    def getVmAttachableDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableDataVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachAliyunKeyAction()
=======
    def addMonToFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachBackupStorageFromZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachBackupStorageFromZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachBackupStorageFromZoneAction()
=======
    def changeBackupStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeBackupStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeBackupStorageStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachDataVolumeFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachDataVolumeFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachDataVolumeFromVmAction()
=======
    def syncEcsVpcFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVpcFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVpcFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachEipAction()
=======
    def queryLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapBindingAction()
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
    def detachIsoFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachIsoFromVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachIsoFromVmInstanceAction()
=======
    def detachL2NetworkFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL2NetworkFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL2NetworkFromClusterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachL2NetworkFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL2NetworkFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL2NetworkFromClusterAction()
=======
    def createDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachL3NetworkFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL3NetworkFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL3NetworkFromVmAction()
=======
    def addConnectionAccessPointFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddConnectionAccessPointFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddConnectionAccessPointFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachNetworkServiceFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction()
=======
    def getIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIdentityZoneFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachOssBucketToEcsDataCenterAction()
=======
    def createCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateClusterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachPoliciesFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPoliciesFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPoliciesFromUserAction()
=======
    def getVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeQosAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachPolicyFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserAction()
=======
    def updateFusionstorBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorBackupStorageMonAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachPolicyFromUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserGroupAction()
=======
    def addCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPortForwardingRuleAction()
=======
    def getCandidateVmForAttachingIso(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmForAttachingIsoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmForAttachingIsoAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachPrimaryStorageFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPrimaryStorageFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPrimaryStorageFromClusterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def detachSecurityGroupFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction()
=======
    def getHypervisorTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHypervisorTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHypervisorTypesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def exportImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExportImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExportImageFromBackupStorageAction()
=======
    def createIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateIPsecConnectionAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def expungeDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeDataVolumeAction()
=======
    def getPortForwardingAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPortForwardingAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPortForwardingAttachableVmNicsAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def expungeImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeImageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeImageAction()
=======
    def setVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmInstanceHaLevelAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def expungeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeVmInstanceAction()
=======
    def addVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVCenterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getAccountQuotaUsage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAccountQuotaUsageAction.class) Closure c) {
        def a = new org.zstack.sdk.GetAccountQuotaUsageAction()
=======
    def changeResourceOwner(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeResourceOwnerAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeResourceOwnerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getBackupStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageCapacityAction()
=======
    def getVolumeCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeCapabilitiesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getBackupStorageForCreatingImageFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction()
=======
    def deleteIdentityZoneInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIdentityZoneInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIdentityZoneInLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getBackupStorageForCreatingImageFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction()
=======
    def deleteOssFileBucketNameInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssFileBucketNameInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssFileBucketNameInLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getBackupStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageTypesAction()
=======
    def expungeImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeImageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeImageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getCandidateIsoForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateIsoForAttachingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateIsoForAttachingVmAction()
=======
    def createStopVmInstanceScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateStopVmInstanceSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateStopVmInstanceSchedulerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getCandidateVmForAttachingIso(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmForAttachingIsoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmForAttachingIsoAction()
=======
    def queryLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapServerAction()
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
    def getCandidateVmNicForSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction()
=======
    def queryBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBackupStorageAction()
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
    def getCandidateVmNicsForLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction()
=======
    def queryVirtualBorderRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction()
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
    def getCandidateZonesClustersHostsForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction()
=======
    def createL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getCpuMemoryCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCpuMemoryCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCpuMemoryCapacityAction()
=======
    def reconnectBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getCurrentTime(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCurrentTimeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCurrentTimeAction()
        
=======
    def reconnectImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectImageStoreBackupStorageAction()
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
    def getDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetDataCenterFromRemoteAction()
=======
    def deleteNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNicQosAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getDataVolumeAttachableVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDataVolumeAttachableVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetDataVolumeAttachableVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getEcsInstanceVncUrl(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEcsInstanceVncUrlAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEcsInstanceVncUrlAction()
=======
    def getVmAttachableL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getEipAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEipAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEipAttachableVmNicsAction()
=======
    def terminateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getFreeIpOfIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfIpRangeAction()
=======
    def addLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLdapServerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getFreeIpOfL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfL3NetworkAction()
=======
    def addCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getHostAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHostAllocatorStrategiesAction()
=======
    def deleteResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteResourcePriceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getHypervisorTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHypervisorTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHypervisorTypesAction()
=======
    def deleteLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapServerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIdentityZoneFromRemoteAction()
=======
    def deleteBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetImageQgaAction()
=======
    def attachPoliciesToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPoliciesToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPoliciesToUserAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getInterdependentL3NetworksImages(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetInterdependentL3NetworksImagesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetInterdependentL3NetworksImagesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getIpAddressCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIpAddressCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIpAddressCapacityAction()
=======
    def addFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getL2NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL2NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL2NetworkTypesAction()
=======
    def getImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetImageQgaAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getL3NetworkDhcpIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkDhcpIpAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkDhcpIpAddressAction()
=======
    def deleteEcsVpcInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcInLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getL3NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkTypesAction()
=======
    def deleteVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVipAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getLicenseCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLicenseCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseCapabilitiesAction()
=======
    def attachIsoToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachIsoToVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachIsoToVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getLicenseInfo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLicenseInfoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseInfoAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getLocalStorageHostDiskCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLocalStorageHostDiskCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLocalStorageHostDiskCapacityAction()
=======
    def addMonToCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getNetworkServiceTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNetworkServiceTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNetworkServiceTypesAction()
=======
    def deleteDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDiskOfferingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNicQosAction()
=======
    def queryEcsSecurityGroupFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction()
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
    def getOssBucketNameFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOssBucketNameFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetOssBucketNameFromRemoteAction()
=======
    def reclaimSpaceFromImageStore(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReclaimSpaceFromImageStoreAction.class) Closure c) {
        def a = new org.zstack.sdk.ReclaimSpaceFromImageStoreAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getPortForwardingAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPortForwardingAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPortForwardingAttachableVmNicsAction()
=======
    def addNfsPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddNfsPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddNfsPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getPrimaryStorageAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction()
=======
    def queryVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageCapacityAction()
=======
    def cloneVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CloneVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CloneVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getPrimaryStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageTypesAction()
=======
    def reconnectHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectHostAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getResourceNames(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceNamesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetResourceNamesAction()
=======
    def deleteGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteGCJobAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getTaskProgress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTaskProgressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetTaskProgressAction()
=======
    def attachDataVolumeToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachDataVolumeToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachDataVolumeToVmAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVersionAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVersionAction()
        
=======
    def addCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStoragePoolAction()
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
    def getVmAttachableDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableDataVolumeAction()
=======
    def deleteInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteInstanceOfferingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVmAttachableL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableL3NetworkAction()
=======
    def querySftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySftpBackupStorageAction()
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
    def getVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmBootOrderAction()
=======
    def queryImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageAction()
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
    def getVmCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmCapabilitiesAction()
=======
    def getFreeIpOfIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfIpRangeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVmConsoleAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsoleAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsoleAddressAction()
=======
    def createVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsolePasswordAction()
=======
    def addKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddKVMHostAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmHostnameAction()
=======
    def attachNetworkServiceToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachNetworkServiceToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachNetworkServiceToL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmInstanceHaLevelAction()
=======
    def updateUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVmMigrationCandidateHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmMigrationCandidateHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmMigrationCandidateHostsAction()
=======
    def addImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmQgaAction()
=======
    def syncEcsSecurityGroupFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmSshKeyAction()
=======
    def queryImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageStoreBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVmStartingCandidateClustersHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmStartingCandidateClustersHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmStartingCandidateClustersHostsAction()
=======
    def detachNetworkServiceFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVolumeCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeCapabilitiesAction()
=======
    def queryPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPolicyAction()
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
    def getVolumeFormat(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeFormatAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeFormatAction()
=======
    def deleteVirtualRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualRouterLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def getVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeQosAction()
=======
    def deleteCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteCephPrimaryStoragePoolAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def isReadyToGo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsReadyToGoAction.class) Closure c) {
        def a = new org.zstack.sdk.IsReadyToGoAction()
=======
    def getResourceNames(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceNamesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetResourceNamesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def kvmRunShell(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.KvmRunShellAction.class) Closure c) {
        def a = new org.zstack.sdk.KvmRunShellAction()
=======
    def updateSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSftpBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def localStorageGetVolumeMigratableHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction()
=======
    def createDataVolumeFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def localStorageMigrateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageMigrateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageMigrateVolumeAction()
=======
    def attachL3NetworkToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL3NetworkToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL3NetworkToVmAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def logInByAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByAccountAction()
        
=======
    def queryZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryZoneAction()
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
    def logInByLdap(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByLdapAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByLdapAction()
        
=======
    def queryRouterInterfaceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryRouterInterfaceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryRouterInterfaceFromLocalAction()
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
    def logInByUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByUserAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByUserAction()
        
=======
    def getVmCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmCapabilitiesAction()
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
    def logOut(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogOutAction.class) Closure c) {
        def a = new org.zstack.sdk.LogOutAction()
        
=======
    def recoverVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverVmInstanceAction()
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
    def migrateVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MigrateVmAction.class) Closure c) {
        def a = new org.zstack.sdk.MigrateVmAction()
=======
    def querySharedResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySharedResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySharedResourceAction()
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
    def pauseVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PauseVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.PauseVmInstanceAction()
=======
    def queryAccountResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountResourceRefAction()
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
    def powerOffBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOffBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerOffBaremetalHostAction()
=======
    def recoveryVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def powerOnBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOnBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerOnBaremetalHostAction()
=======
    def removeMonFromCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def powerResetBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerResetBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerResetBaremetalHostAction()
=======
    def syncEcsVSwitchFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVSwitchFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVSwitchFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def prometheusQueryLabelValues(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryLabelValuesAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryLabelValuesAction()
=======
    def queryLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerAction()
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
    def prometheusQueryMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryMetadataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryMetadataAction()
=======
    def queryEcsVpcFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVpcFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVpcFromLocalAction()
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
    def prometheusQueryPassThrough(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryPassThroughAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryPassThroughAction()
=======
    def destroyVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DestroyVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DestroyVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def prometheusQueryVmMonitoringData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryVmMonitoringDataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryVmMonitoringDataAction()
=======
    def createVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVniRangeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def provisionBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ProvisionBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ProvisionBaremetalHostAction()
=======
    def removeDnsFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveDnsFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveDnsFromL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def queryAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountAction()
=======
    def createRootVolumeTemplateFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction()
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
    def queryAccountResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountResourceRefAction()
=======
    def detachPrimaryStorageFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPrimaryStorageFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPrimaryStorageFromClusterAction()
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
    def queryAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAliyunKeySecretAction()
=======
    def revertVolumeFromSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevertVolumeFromSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.RevertVolumeFromSnapshotAction()
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
    def queryApplianceVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryApplianceVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryApplianceVmAction()
=======
    def queryCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStorageAction()
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
    def queryBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBackupStorageAction()
=======
    def deleteVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVniRangeAction()
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
    def queryBaremetalChessis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalChessisAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalChessisAction()
=======
    def createEcsImageFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsImageFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsImageFromLocalImageAction()
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
    def queryBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalHostCfgAction()
=======
    def queryVirtualRouterVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterVmAction()
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
    def queryBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalPxeServerAction()
=======
    def queryAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountAction()
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
    def queryCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephBackupStorageAction()
=======
    def rebootEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootEcsInstanceAction()
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
    def queryCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStorageAction()
=======
    def queryLocalStorageResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLocalStorageResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLocalStorageResourceRefAction()
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
    def queryCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStoragePoolAction()
=======
    def queryVCenterCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterClusterAction()
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
    def queryCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryClusterAction()
=======
    def getVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmSshKeyAction()
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
    def queryConnectionAccessPointFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConnectionAccessPointFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConnectionAccessPointFromLocalAction()
=======
    def createL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL3NetworkAction()
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
    def queryConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConsoleProxyAgentAction()
=======
    def addMonToCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephPrimaryStorageAction()
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
    def queryDataCenterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDataCenterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDataCenterFromLocalAction()
=======
    def addOssFileBucketName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddOssFileBucketNameAction.class) Closure c) {
        def a = new org.zstack.sdk.AddOssFileBucketNameAction()
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
    def queryDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDiskOfferingAction()
=======
    def createPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePolicyAction()
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
    def queryEcsImageFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsImageFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsImageFromLocalAction()
=======
    def isReadyToGo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsReadyToGoAction.class) Closure c) {
        def a = new org.zstack.sdk.IsReadyToGoAction()
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
    def queryEcsInstanceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsInstanceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsInstanceFromLocalAction()
=======
    def updateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction()
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


    def queryEcsSecurityGroupFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupFromLocalAction()
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
    def queryEcsSecurityGroupRuleFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction()
=======
    def deleteEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEipAction()
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
    def queryEcsVSwitchFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVSwitchFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVSwitchFromLocalAction()
=======
    def deleteUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserAction()
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
    def queryEcsVpcFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVpcFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVpcFromLocalAction()
=======
    def deleteEcsSecurityGroupInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction()
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
    def queryEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEipAction()
=======
    def startEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartEcsInstanceAction()
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
    def queryFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorBackupStorageAction()
=======
    def detachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachOssBucketToEcsDataCenterAction()
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
    def queryFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorPrimaryStorageAction()
=======
    def addSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSecurityGroupRuleAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def queryGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGCJobAction()
=======
    def queryFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorPrimaryStorageAction()
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
    def queryGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGlobalConfigAction()
=======
    def createEcsInstanceFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsInstanceFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsInstanceFromLocalImageAction()
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
    def queryHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHostAction()
=======
    def createDataVolumeTemplateFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction()
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
    def queryHybridEipFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHybridEipFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHybridEipFromLocalAction()
=======
    def queryDataCenterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDataCenterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDataCenterFromLocalAction()
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
    def queryIPSecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIPSecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIPSecConnectionAction()
=======
    def queryEcsImageFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsImageFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsImageFromLocalAction()
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
    def queryIdentityZoneFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIdentityZoneFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIdentityZoneFromLocalAction()
=======
    def getEcsInstanceVncUrl(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEcsInstanceVncUrlAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEcsInstanceVncUrlAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def queryImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageAction()
=======
    def addSharedMountPointPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSharedMountPointPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSharedMountPointPrimaryStorageAction()
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
    def queryImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageStoreBackupStorageAction()
=======
    def queryOssBucketFileName(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryOssBucketFileNameAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryOssBucketFileNameAction()
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
    def queryInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryInstanceOfferingAction()
=======
    def syncRouteEntryFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouteEntryFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouteEntryFromRemoteAction()
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
    def queryIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIpRangeAction()
=======
    def createL2NoVlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2NoVlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2NoVlanNetworkAction()
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
    def queryL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2NetworkAction()
=======
    def changeImageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeImageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeImageStateAction()
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
    def queryL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VlanNetworkAction()
=======
    def changePrimaryStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePrimaryStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePrimaryStorageStateAction()
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
    def queryL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkAction()
=======
    def querySecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupAction()
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
    def queryL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkPoolAction()
=======
    def queryGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGCJobAction()
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
    def queryL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL3NetworkAction()
=======
    def queryVCenterBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterBackupStorageAction()
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
    def queryLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapBindingAction()
=======
    def queryInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryInstanceOfferingAction()
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
    def queryLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapServerAction()
=======
    def addVmNicToLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToLoadBalancerAction()
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
    def queryLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerAction()
=======
    def updateBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBackupStorageAction()
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
    def queryLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerListenerAction()
=======
    def getVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmInstanceHaLevelAction()
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
    def queryLocalStorageResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLocalStorageResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLocalStorageResourceRefAction()
=======
    def createVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVipAction()
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
    def queryManagementNode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryManagementNodeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryManagementNodeAction()
=======
    def deleteEcsImageRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageRemoteAction()
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
    def queryNetworkServiceL3NetworkRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction()
=======
    def changePortForwardingRuleState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePortForwardingRuleStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePortForwardingRuleStateAction()
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
    def queryNetworkServiceProvider(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceProviderAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceProviderAction()
=======
    def pauseVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PauseVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.PauseVmInstanceAction()
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


    def queryVCenterPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVCenterPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVCenterPrimaryStorageAction()
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


    def queryVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVipAction()
=======
    def addSimulatorHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorHostAction()
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
    def queryVirtualBorderRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction()
=======
    def addIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIdentityZoneFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def queryVirtualRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterFromLocalAction()
=======
    def getIpAddressCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIpAddressCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIpAddressCapacityAction()
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
    def queryVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterOfferingAction()
=======
    def updateEcsInstanceVncPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEcsInstanceVncPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEcsInstanceVncPasswordAction()
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
    def queryVirtualRouterVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterVmAction()
=======
    def updateVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeSnapshotAction()
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
    def queryVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmInstanceAction()
=======
    def getHostAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHostAllocatorStrategiesAction()
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
    def queryVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicAction()
=======
    def attachBackupStorageToZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachBackupStorageToZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachBackupStorageToZoneAction()
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
    def queryVmNicInSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicInSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicInSecurityGroupAction()
=======
    def addUserToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddUserToGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddUserToGroupAction()
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
    def queryVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVniRangeAction()
=======
    def queryGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGlobalConfigAction()
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
    def queryVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeAction()
=======
    def changeClusterState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeClusterStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeClusterStateAction()
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
    def queryVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotAction()
=======
    def getPrimaryStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageTypesAction()
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
    def queryVolumeSnapshotTree(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotTreeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotTreeAction()
=======
    def queryHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHostAction()
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
    def queryWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryWebhookAction()
=======
    def updateInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateInstanceOfferingAction()
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
    def queryZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryZoneAction()
=======
    def updateQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateQuotaAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def rebootEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootEcsInstanceAction()
=======
    def cleanUpImageCacheOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def rebootVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootVmInstanceAction()
=======
    def queryL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL3NetworkAction()
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
    def reclaimSpaceFromImageStore(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReclaimSpaceFromImageStoreAction.class) Closure c) {
        def a = new org.zstack.sdk.ReclaimSpaceFromImageStoreAction()
=======
    def detachPolicyFromUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def reconnectBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectBackupStorageAction()
=======
    def changeInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def reconnectConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectConsoleProxyAgentAction()
=======
    def setVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmBootOrderAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def reconnectHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectHostAction()
=======
    def deleteExportedImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteExportedImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteExportedImageFromBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def reconnectImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectImageStoreBackupStorageAction()
=======
    def recoverDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverDataVolumeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def reconnectPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectPrimaryStorageAction()
=======
    def getVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmQgaAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def reconnectVirtualRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectVirtualRouterAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectVirtualRouterAction()
=======
    def setVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmConsolePasswordAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def recoverDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverDataVolumeAction()
=======
    def queryVmNicInSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicInSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicInSecurityGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def recoverImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverImageAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverImageAction()
=======
    def updateVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualRouterOfferingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def recoverVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverVmInstanceAction()
=======
    def updateGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateGlobalConfigAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def recoveryVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction()
=======
    def updatePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def refreshLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RefreshLoadBalancerAction()
=======
    def stopVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def reimageVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReimageVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ReimageVmInstanceAction()
=======
    def deleteVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmInstanceHaLevelAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def reloadLicense(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReloadLicenseAction.class) Closure c) {
        def a = new org.zstack.sdk.ReloadLicenseAction()
=======
    def getL2NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL2NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL2NetworkTypesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def removeDnsFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveDnsFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveDnsFromL3NetworkAction()
=======
    def createResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateResourcePriceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def removeMonFromCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephBackupStorageAction()
=======
    def attachPolicyToUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPolicyToUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def removeMonFromCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction()
=======
    def updateSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSecurityGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def removeMonFromFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction()
=======
    def prometheusQueryPassThrough(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryPassThroughAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryPassThroughAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def removeMonFromFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction()
=======
    def addDnsToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDnsToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDnsToL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def removeUserFromGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveUserFromGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveUserFromGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def removeVmNicFromLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVmNicFromLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveVmNicFromLoadBalancerAction()
=======
    def reconnectVirtualRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectVirtualRouterAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectVirtualRouterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def requestConsoleAccess(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RequestConsoleAccessAction.class) Closure c) {
        def a = new org.zstack.sdk.RequestConsoleAccessAction()
=======
    def queryL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkPoolAction()
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
    def resumeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResumeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ResumeVmInstanceAction()
=======
    def expungeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def revertVolumeFromSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevertVolumeFromSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.RevertVolumeFromSnapshotAction()
=======
    def deleteEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def revokeResourceSharing(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevokeResourceSharingAction.class) Closure c) {
        def a = new org.zstack.sdk.RevokeResourceSharingAction()
=======
    def updateCephPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephPrimaryStorageMonAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def setImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetImageQgaAction()
=======
    def updateAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateAccountAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def setNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetNicQosAction()
=======
    def deleteScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSchedulerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def setVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmBootOrderAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def logInByAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByAccountAction()
        
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
    def setVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmConsolePasswordAction()
=======
    def updateImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def setVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmHostnameAction()
=======
    def deleteUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def setVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmInstanceHaLevelAction()
=======
    def checkApiPermission(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckApiPermissionAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckApiPermissionAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def setVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmQgaAction()
=======
    def queryNotificationSubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationSubscriptionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationSubscriptionAction()
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
    def setVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmSshKeyAction()
=======
    def deleteHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHostAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteHostAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def setVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmStaticIpAction()
=======
    def queryResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryResourcePriceAction()
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
    def setVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVolumeQosAction()
=======
    def stopEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopEcsInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def shareResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ShareResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.ShareResourceAction()
=======
    def createAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateAccountAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def startBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.StartBaremetalPxeServerAction()
=======
    def kvmRunShell(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.KvmRunShellAction.class) Closure c) {
        def a = new org.zstack.sdk.KvmRunShellAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def startEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartEcsInstanceAction()
=======
    def deleteLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def startVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartVmInstanceAction()
=======
    def reconnectConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectConsoleProxyAgentAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def stopBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.StopBaremetalPxeServerAction()
=======
    def getVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsolePasswordAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def stopEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopEcsInstanceAction()
=======
    def addIpRangeByNetworkCidr(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeByNetworkCidrAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeByNetworkCidrAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def stopVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopVmInstanceAction()
=======
    def updateImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageStoreBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncEcsImageFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsImageFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsImageFromRemoteAction()
=======
    def setVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmHostnameAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncEcsInstanceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsInstanceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsInstanceFromRemoteAction()
=======
    def updateFusionstorPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncEcsSecurityGroupFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction()
=======
    def attachPrimaryStorageToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPrimaryStorageToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPrimaryStorageToClusterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncEcsVSwitchFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVSwitchFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVSwitchFromRemoteAction()
=======
    def revokeResourceSharing(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevokeResourceSharingAction.class) Closure c) {
        def a = new org.zstack.sdk.RevokeResourceSharingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncEcsVpcFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVpcFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVpcFromRemoteAction()
=======
    def reconnectPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectPrimaryStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncImageSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncImageSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncImageSizeAction()
=======
    def deleteAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAccountAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncPrimaryStorageCapacityAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def getVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVersionAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVersionAction()
        
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
    def syncRouteEntryFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouteEntryFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouteEntryFromRemoteAction()
=======
    def deletePolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePolicyAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncRouterInterfaceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouterInterfaceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouterInterfaceFromRemoteAction()
=======
    def changeSecurityGroupState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSecurityGroupStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSecurityGroupStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncVirtualBorderRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction()
=======
    def getCandidateZonesClustersHostsForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncVirtualRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualRouterFromRemoteAction()
=======
    def getCandidateVmNicsForLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def syncVolumeSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVolumeSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVolumeSizeAction()
=======
    def deleteSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def terminateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction()
=======
    def queryRouteEntryFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryRouteEntryFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryRouteEntryFromLocalAction()
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
    def triggerGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TriggerGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.TriggerGCJobAction()
=======
    def queryPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPortForwardingRuleAction()
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
    def updateAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateAccountAction()
=======
    def querySecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySecurityGroupRuleAction()
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
    def updateBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBackupStorageAction()
=======
    def addImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageStoreBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateBaremetalChessis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalChessisAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBaremetalChessisAction()
=======
    def addMonToFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorBackupStorageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBaremetalHostCfgAction()
=======
    def updateKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateKVMHostAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBaremetalPxeServerAction()
=======
    def deleteVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmHostnameAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateCephBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephBackupStorageMonAction()
=======
    def createVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVirtualRouterOfferingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateCephPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephPrimaryStorageMonAction()
=======
    def deleteVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmConsolePasswordAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateClusterAction()
=======
    def queryCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryClusterAction()
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
    def updateDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateDiskOfferingAction()
=======
    def updateSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSystemTagAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateEcsInstanceVncPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEcsInstanceVncPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEcsInstanceVncPasswordAction()
=======
    def attachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachOssBucketToEcsDataCenterAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEipAction()
=======
    def getEipAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEipAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEipAttachableVmNicsAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateFusionstorBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorBackupStorageMonAction()
=======
    def createUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateFusionstorPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction()
=======
    def syncImageSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncImageSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncImageSizeAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateGlobalConfigAction()
=======
    def attachSecurityGroupToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachSecurityGroupToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachSecurityGroupToL3NetworkAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateHostAction()
=======
    def getLocalStorageHostDiskCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLocalStorageHostDiskCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLocalStorageHostDiskCapacityAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageAction()
=======
    def updatePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePortForwardingRuleAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageStoreBackupStorageAction()
=======
    def queryEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEipAction()
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
    def updateInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateInstanceOfferingAction()
=======
    def queryUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserAction()
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
    def updateIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateIpRangeAction()
=======
    def syncPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncPrimaryStorageCapacityAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateKVMHostAction()
=======
    def deleteVmNicFromSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmNicFromSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmNicFromSecurityGroupAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL2NetworkAction()
=======
    def getPrimaryStorageAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL3NetworkAction()
=======
    def getBackupStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageCapacityAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateLdapServerAction()
=======
    def setVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVolumeQosAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateNotificationsStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateNotificationsStatusAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateNotificationsStatusAction()
=======
    def queryUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserTagAction()
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
    def updatePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePortForwardingRuleAction()
=======
    def deleteEcsImageLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageLocalAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updatePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePrimaryStorageAction()
=======
    def queryNetworkServiceL3NetworkRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateQuotaAction()
=======
    def addDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDataCenterFromRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateRouteInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateRouteInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateRouteInterfaceRemoteAction()
=======
    def querySystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySystemTagAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateScheduler(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSchedulerAction()
=======
    def queryIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIpRangeAction()
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
    def updateSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSecurityGroupAction()
=======
    def deleteSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupRuleAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSftpBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
=======
    def validateSession(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateSessionAction.class) Closure c) {
        def a = new org.zstack.sdk.ValidateSessionAction()
        
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
    def updateSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSystemTagAction()
=======
    def queryUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryUserGroupAction()
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
    def updateUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserAction()
=======
    def createVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVmInstanceAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserGroupAction()
=======
    def queryConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConsoleProxyAgentAction()
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
    def updateVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVipAction()
=======
    def detachBackupStorageFromZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachBackupStorageFromZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachBackupStorageFromZoneAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction()
=======
    def deleteEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcRemoteAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualRouterOfferingAction()
=======
    def createDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDiskOfferingAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVmInstanceAction()
=======
    def detachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachEipAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeAction()
=======
    def recoverImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverImageAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverImageAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeSnapshotAction()
=======
    def changeInstanceOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingStateAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateWebhookAction()
=======
    def removeVmNicFromLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVmNicFromLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveVmNicFromLoadBalancerAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def updateZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateZoneAction()
=======
    def getBackupStorageForCreatingImageFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction()
>>>>>>> update sdks for hybrid
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


<<<<<<< HEAD
    def validateSession(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateSessionAction.class) Closure c) {
        def a = new org.zstack.sdk.ValidateSessionAction()
        
=======
    def createEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRemoteAction()
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
