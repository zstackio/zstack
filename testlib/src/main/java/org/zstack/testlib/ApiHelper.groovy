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
    
        def addAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.AddAliyunKeySecretAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.AddCephPrimaryStoragePoolAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addConnectionAccessPointFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddConnectionAccessPointFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddConnectionAccessPointFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDataCenterFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addDnsToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddDnsToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AddDnsToL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddFusionstorPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIdentityZoneFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddImageStoreBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addIpRangeByNetworkCidr(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddIpRangeByNetworkCidrAction.class) Closure c) {
        def a = new org.zstack.sdk.AddIpRangeByNetworkCidrAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddKVMHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLdapServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addLocalPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddLocalPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddLocalPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addMonToCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addMonToCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToCephPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addMonToFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addMonToFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddMonToFusionstorPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addNfsPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddNfsPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddNfsPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addOssBucketFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddOssBucketFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.AddOssBucketFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addSchedulerJobToSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSchedulerJobToSchedulerTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSchedulerJobToSchedulerTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSecurityGroupRuleAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSftpBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addSharedMountPointPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSharedMountPointPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSharedMountPointPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addSimulatorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addSimulatorHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorHostAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addSimulatorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddSimulatorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddSimulatorPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addUserToGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddUserToGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddUserToGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVCenterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addVRouterRouteEntry(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVRouterRouteEntryAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVRouterRouteEntryAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addVmNicToLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToLoadBalancerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addVmNicToSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddVmNicToSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.AddVmNicToSecurityGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addXSkyPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddXSkyPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddXSkyPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def addZsesPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AddZsesPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.AddZsesPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachAliyunKeyAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachBackupStorageToZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachBackupStorageToZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachBackupStorageToZoneAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachDataVolumeToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachDataVolumeToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachDataVolumeToVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachEipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachHybridEipToEcs(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachHybridEipToEcsAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachHybridEipToEcsAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachIsoToVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachIsoToVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachIsoToVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachL2NetworkToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL2NetworkToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL2NetworkToClusterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachL3NetworkToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachL3NetworkToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachL3NetworkToVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachMonitorTriggerToTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachMonitorTriggerActionToTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachMonitorTriggerActionToTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachNetworkServiceToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachNetworkServiceToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachNetworkServiceToL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachOssBucketToEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachOssBucketToEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachOssBucketToEcsDataCenterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachPciDeviceToVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPciDeviceToVmAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPciDeviceToVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachPoliciesToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPoliciesToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPoliciesToUserAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachPolicyToUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPolicyToUserAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPolicyToUserAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def attachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPortForwardingRuleAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachPrimaryStorageToCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachPrimaryStorageToClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachPrimaryStorageToClusterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachSecurityGroupToL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachSecurityGroupToL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachSecurityGroupToL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def attachVRouterRouteTableToVRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.AttachVRouterRouteTableToVRouterAction.class) Closure c) {
        def a = new org.zstack.sdk.AttachVRouterRouteTableToVRouterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def backupDatabaseToPublicCloud(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.BackupDatabaseToPublicCloudAction.class) Closure c) {
        def a = new org.zstack.sdk.BackupDatabaseToPublicCloudAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def calculateAccountSpending(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CalculateAccountSpendingAction.class) Closure c) {
        def a = new org.zstack.sdk.CalculateAccountSpendingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeBackupStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeBackupStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeBackupStorageStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeClusterState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeClusterStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeClusterStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeDiskOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeDiskOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeDiskOfferingStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeEipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeEipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeEipStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeHostState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeHostStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeHostStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeImageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeImageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeImageStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeInstanceOfferingState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeInstanceOfferingStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeInstanceOfferingStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeL3NetworkState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeL3NetworkStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeL3NetworkStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeMediaState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeMediaStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeMediaStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeMonitorTriggerStateAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeMonitorTriggerActionStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeMonitorTriggerActionStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeMonitorTriggerState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeMonitorTriggerStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeMonitorTriggerStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changePortForwardingRuleState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePortForwardingRuleStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePortForwardingRuleStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changePrimaryStorageState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangePrimaryStorageStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangePrimaryStorageStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeResourceOwner(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeResourceOwnerAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeResourceOwnerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeSchedulerState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSchedulerStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSchedulerStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeSecurityGroupState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeSecurityGroupStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeSecurityGroupStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeVipState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVipStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVipStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeVmPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVmPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVmPasswordAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeVolumeState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeVolumeStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeVolumeStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def changeZoneState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ChangeZoneStateAction.class) Closure c) {
        def a = new org.zstack.sdk.ChangeZoneStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def checkApiPermission(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckApiPermissionAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckApiPermissionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def checkIpAvailability(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CheckIpAvailabilityAction.class) Closure c) {
        def a = new org.zstack.sdk.CheckIpAvailabilityAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def cleanInvalidLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanInvalidLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanInvalidLdapBindingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def cleanUpImageCacheOnPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.CleanUpImageCacheOnPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def cloneVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CloneVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CloneVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateAccountAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createAliyunVpcVirtualRouterEntryRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateAliyunVpcVirtualRouterEntryRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateAliyunVpcVirtualRouterEntryRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalChassisAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateBaremetalChassisAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateBaremetalHostCfgAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateBaremetalPxeServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateClusterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createConnectionBetweenL3NetworkAndAliyunVSwitch(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateConnectionBetweenL3NetworkAndAliyunVSwitchAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateConnectionBetweenL3NetworkAndAliyunVSwitchAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createDataVolumeFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeSnapshotAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createDataVolumeFromVolumeTemplate(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeFromVolumeTemplateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createDataVolumeTemplateFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDataVolumeTemplateFromVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateDiskOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createEcsImageFromLocalImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsImageFromLocalImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsImageFromLocalImageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createEcsInstanceFromEcsImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsInstanceFromEcsImageAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsInstanceFromEcsImageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createEcsSecurityGroupRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsSecurityGroupRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsSecurityGroupRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def createEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVSwitchRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEcsVpcRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createEmailMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEmailMediaAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEmailMediaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createEmailMonitorTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateEmailMonitorTriggerActionAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateEmailMonitorTriggerActionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createHybridEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateHybridEipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateHybridEipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateIPsecConnectionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateInstanceOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createL2NoVlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2NoVlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2NoVlanNetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VlanNetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL2VxlanNetworkPoolAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLdapBindingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateLoadBalancerListenerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createMonitorTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateMonitorTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateMonitorTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createOssBackupBucketRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateOssBackupBucketRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateOssBackupBucketRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createOssBucketRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateOssBucketRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateOssBucketRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createPciDeviceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePciDeviceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePciDeviceOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createPolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreatePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.CreatePolicyAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def createResourcePrice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateResourcePriceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateResourcePriceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createRootVolumeTemplateFromRootVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromRootVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createRootVolumeTemplateFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRootVolumeTemplateFromVolumeSnapshotAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createRouterInterfacePairRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateRouterInterfacePairRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateRouterInterfacePairRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createSchedulerJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSchedulerJobAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSchedulerJobAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSchedulerTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSchedulerTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSecurityGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateSystemTagAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createUserTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateUserTagAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateUserTagAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVRouterRouteTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVRouterRouteTableAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVRouterRouteTableAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVirtualRouterOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVniRangeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVolumeSnapshotAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVpcUserVpnGatewayRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVpcUserVpnGatewayRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVpcUserVpnGatewayRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVpcVpnConnectionRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVpcVpnConnectionRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVpcVpnConnectionRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVpnIkeConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVpnIkeConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVpnIkeConfigAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createVpnIpsecConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateVpnIpsecConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateVpnIpsecConfigAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateWebhookAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def createZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.CreateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.CreateZoneAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAccountAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteAlert(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAlertAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAlertAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAliyunKeySecretAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteAliyunRouteEntryRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAliyunRouteEntryRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAliyunRouteEntryRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteAllEcsInstancesFromDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteAllEcsInstancesFromDataCenterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteBackupFileInPublic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBackupFileInPublicAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBackupFileInPublicAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalChassisAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBaremetalChassisAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBaremetalHostCfgAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteBaremetalPxeServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteCephPrimaryStoragePoolAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteClusterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteConnectionAccessPointLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteConnectionAccessPointLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteConnectionAccessPointLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteConnectionBetweenL3NetWorkAndAliyunVSwitch(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteConnectionBetweenL3NetWorkAndAliyunVSwitchAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteConnectionBetweenL3NetWorkAndAliyunVSwitchAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteDataCenterInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataCenterInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataCenterInLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDataVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteDiskOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEcsImageLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEcsImageRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsImageRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsImageRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEcsInstanceLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsInstanceLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsInstanceLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEcsSecurityGroupInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupInLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


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


    def deleteEcsSecurityGroupRuleRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsSecurityGroupRuleRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEcsVSwitchInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchInLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEcsVSwitchRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVSwitchRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVSwitchRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEcsVpcInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcInLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEcsVpcRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEcsVpcRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEcsVpcRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteEipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteExportedImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteExportedImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteExportedImageFromBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteGCJobAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHostAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteHybridEipFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHybridEipFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteHybridEipFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteHybridEipRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteHybridEipRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteHybridEipRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIPsecConnectionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteIdentityZoneInLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIdentityZoneInLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIdentityZoneInLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteImageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteImageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteInstanceOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteIpRangeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL2NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapBindingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLdapServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteLoadBalancerListenerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteMediaAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteMediaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteMonitorTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteMonitorTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteMonitorTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteMonitorTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteMonitorTriggerActionAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteMonitorTriggerActionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNicQosAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteNotifications(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteNotificationsAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteNotificationsAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteOssBucketFileRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssBucketFileRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssBucketFileRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteOssBucketNameLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssBucketNameLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssBucketNameLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteOssBucketRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteOssBucketRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteOssBucketRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deletePciDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePciDeviceAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePciDeviceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deletePciDeviceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePciDeviceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePciDeviceOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deletePolicy(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePolicyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePolicyAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deletePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePortForwardingRuleAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deletePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeletePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.DeletePrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def deleteRouterInterfaceLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteRouterInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteRouterInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteRouterInterfaceRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteSchedulerJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerJobAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSchedulerJobAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSchedulerTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSchedulerTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteSecurityGroupRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteSecurityGroupRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteSecurityGroupRuleAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteTagAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteTagAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteUserGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVCenterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVRouterRouteEntry(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVRouterRouteEntryAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVRouterRouteEntryAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVRouterRouteTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVRouterRouteTableAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVRouterRouteTableAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVipAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVipQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVipQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVipQosAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVirtualBorderRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualBorderRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualBorderRouterLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVirtualRouterLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVirtualRouterLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVirtualRouterLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmConsolePasswordAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmHostnameAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmInstanceHaLevelAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVmNicFromSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmNicFromSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmNicFromSecurityGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmSshKeyAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVmStaticIpAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVniRangeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeQosAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVolumeSnapshotAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVpcIkeConfigLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVpcIkeConfigLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVpcIkeConfigLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVpcIpSecConfigLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVpcIpSecConfigLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVpcIpSecConfigLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVpcUserVpnGatewayLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVpcUserVpnGatewayLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVpcUserVpnGatewayLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVpcUserVpnGatewayRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVpcUserVpnGatewayRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVpcUserVpnGatewayRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVpcVpnConnectionLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVpcVpnConnectionLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVpcVpnConnectionLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVpcVpnConnectionRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVpcVpnConnectionRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVpcVpnConnectionRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteVpcVpnGatewayLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteVpcVpnGatewayLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteVpcVpnGatewayLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteWebhookAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def deleteZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DeleteZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DeleteZoneAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def destroyVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DestroyVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DestroyVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachAliyunKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachAliyunKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachAliyunKeyAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachBackupStorageFromZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachBackupStorageFromZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachBackupStorageFromZoneAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachDataVolumeFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachDataVolumeFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachDataVolumeFromVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachEipAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachEipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachHybridEipFromEcs(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachHybridEipFromEcsAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachHybridEipFromEcsAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachIsoFromVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachIsoFromVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachIsoFromVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachL2NetworkFromCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL2NetworkFromClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL2NetworkFromClusterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachL3NetworkFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachL3NetworkFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachL3NetworkFromVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachMonitorTriggerFromTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachMonitorTriggerActionFromTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachMonitorTriggerActionFromTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachNetworkServiceFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachNetworkServiceFromL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachOssBucketFromEcsDataCenter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachOssBucketFromEcsDataCenterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachOssBucketFromEcsDataCenterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachPciDeviceFromVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPciDeviceFromVmAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPciDeviceFromVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachPoliciesFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPoliciesFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPoliciesFromUserAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachPolicyFromUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachPolicyFromUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPolicyFromUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPolicyFromUserGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachPortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachPortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachPortForwardingRuleAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def detachSecurityGroupFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachSecurityGroupFromL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def detachVRouterRouteTableFromVRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DetachVRouterRouteTableFromVRouterAction.class) Closure c) {
        def a = new org.zstack.sdk.DetachVRouterRouteTableFromVRouterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def downloadBackupFileFromPublicCloud(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.DownloadBackupFileFromPublicCloudAction.class) Closure c) {
        def a = new org.zstack.sdk.DownloadBackupFileFromPublicCloudAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def exportImageFromBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExportImageFromBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExportImageFromBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def expungeDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeDataVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def expungeImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ExpungeImageAction.class) Closure c) {
        def a = new org.zstack.sdk.ExpungeImageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def getAccountQuotaUsage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetAccountQuotaUsageAction.class) Closure c) {
        def a = new org.zstack.sdk.GetAccountQuotaUsageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getBackupStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageCapacityAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getBackupStorageForCreatingImageFromVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getBackupStorageForCreatingImageFromVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageForCreatingImageFromVolumeSnapshotAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getBackupStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetBackupStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetBackupStorageTypesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getCandidateIsoForAttachingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateIsoForAttachingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateIsoForAttachingVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getCandidatePrimaryStoragesForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidatePrimaryStoragesForCreatingVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getCandidateVmForAttachingIso(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmForAttachingIsoAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmForAttachingIsoAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getCandidateVmNicForSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicForSecurityGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getCandidateVmNicsForLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateVmNicsForLoadBalancerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getCandidateZonesClustersHostsForCreatingVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCandidateZonesClustersHostsForCreatingVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getConnectionAccessPointFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetConnectionAccessPointFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetConnectionAccessPointFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getConnectionBetweenL3NetworkAndAliyunVSwitch(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetConnectionBetweenL3NetworkAndAliyunVSwitchAction.class) Closure c) {
        def a = new org.zstack.sdk.GetConnectionBetweenL3NetworkAndAliyunVSwitchAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getCpuMemoryCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCpuMemoryCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCpuMemoryCapacityAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getCreateEcsImageProgress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCreateEcsImageProgressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCreateEcsImageProgressAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getCurrentTime(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetCurrentTimeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetCurrentTimeAction()
        
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetDataCenterFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def getEcsInstanceType(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEcsInstanceTypeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEcsInstanceTypeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getEcsInstanceVncUrl(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEcsInstanceVncUrlAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEcsInstanceVncUrlAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getEipAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetEipAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetEipAttachableVmNicsAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getFreeIpOfIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfIpRangeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getFreeIpOfL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetFreeIpOfL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetFreeIpOfL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getHostAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHostAllocatorStrategiesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getHostIommuState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostIommuStateAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHostIommuStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getHostIommuStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHostIommuStatusAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHostIommuStatusAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getHypervisorTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetHypervisorTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetHypervisorTypesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getIdentityZoneFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIdentityZoneFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIdentityZoneFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetImageQgaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def getIpAddressCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetIpAddressCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetIpAddressCapacityAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getL2NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL2NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL2NetworkTypesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getL3NetworkDhcpIpAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkDhcpIpAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkDhcpIpAddressAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getL3NetworkMtu(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkMtuAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkMtuAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getL3NetworkRouterInterfaceIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkRouterInterfaceIpAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkRouterInterfaceIpAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getL3NetworkTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetL3NetworkTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetL3NetworkTypesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getLicenseCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLicenseCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLicenseCapabilitiesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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
        
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getLocalStorageHostDiskCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetLocalStorageHostDiskCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetLocalStorageHostDiskCapacityAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getMonitorItem(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetMonitorItemAction.class) Closure c) {
        def a = new org.zstack.sdk.GetMonitorItemAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getNetworkServiceTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNetworkServiceTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNetworkServiceTypesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetNicQosAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getOssBackupBucketFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOssBackupBucketFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetOssBackupBucketFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getOssBucketFileFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetOssBucketFileFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetOssBucketFileFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getPortForwardingAttachableVmNics(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPortForwardingAttachableVmNicsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPortForwardingAttachableVmNicsAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getPrimaryStorageAllocatorStrategies(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageAllocatorStrategiesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageCapacityAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getPrimaryStorageTypes(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetPrimaryStorageTypesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetPrimaryStorageTypesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getResourceAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.GetResourceAccountAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getResourceNames(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetResourceNamesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetResourceNamesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getTaskProgress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetTaskProgressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetTaskProgressAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVRouterRouteTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVRouterRouteTableAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVRouterRouteTableAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVersionAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVersionAction()
        
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVipQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVipQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVipQosAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmAttachableDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableDataVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmAttachableL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmAttachableL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmAttachableL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmBootOrderAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmCapabilitiesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmConsoleAddress(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsoleAddressAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsoleAddressAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmConsolePasswordAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmHostnameAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmInstanceHaLevelAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmMigrationCandidateHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmMigrationCandidateHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmMigrationCandidateHostsAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmMonitorNumber(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmMonitorNumberAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmMonitorNumberAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmQgaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmRDP(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmRDPAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmRDPAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmSshKeyAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmStartingCandidateClustersHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmStartingCandidateClustersHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmStartingCandidateClustersHostsAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVmUsbRedirect(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVmUsbRedirectAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVmUsbRedirectAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVolumeCapabilities(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeCapabilitiesAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeCapabilitiesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVolumeFormat(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeFormatAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeFormatAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVolumeQosAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def getVpcVpnConfigurationFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.GetVpcVpnConfigurationFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.GetVpcVpnConfigurationFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def isOpensourceVersion(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsOpensourceVersionAction.class) Closure c) {
        def a = new org.zstack.sdk.IsOpensourceVersionAction()
        
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def isReadyToGo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.IsReadyToGoAction.class) Closure c) {
        def a = new org.zstack.sdk.IsReadyToGoAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def kvmRunShell(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.KvmRunShellAction.class) Closure c) {
        def a = new org.zstack.sdk.KvmRunShellAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def localStorageGetVolumeMigratableHosts(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageGetVolumeMigratableHostsAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def localStorageMigrateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LocalStorageMigrateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.LocalStorageMigrateVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def logInByAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByAccountAction()
        
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def logInByLdap(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByLdapAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByLdapAction()
        
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def logInByUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogInByUserAction.class) Closure c) {
        def a = new org.zstack.sdk.LogInByUserAction()
        
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def logOut(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.LogOutAction.class) Closure c) {
        def a = new org.zstack.sdk.LogOutAction()
        
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def migrateVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.MigrateVmAction.class) Closure c) {
        def a = new org.zstack.sdk.MigrateVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def pauseVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PauseVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.PauseVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def powerOffBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOffBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerOffBaremetalHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def powerOnBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerOnBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerOnBaremetalHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def powerResetBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerResetBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerResetBaremetalHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def powerStatusBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PowerStatusBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.PowerStatusBaremetalHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def prometheusQueryLabelValues(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryLabelValuesAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryLabelValuesAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def prometheusQueryMetadata(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryMetadataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryMetadataAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def prometheusQueryPassThrough(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryPassThroughAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryPassThroughAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def prometheusQueryVmMonitoringData(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.PrometheusQueryVmMonitoringDataAction.class) Closure c) {
        def a = new org.zstack.sdk.PrometheusQueryVmMonitoringDataAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def provisionBaremetalHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ProvisionBaremetalHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ProvisionBaremetalHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryAccountResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAccountResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAccountResourceRefAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryAlert(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAlertAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAlertAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAliyunKeySecretAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryAliyunRouteEntryFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAliyunRouteEntryFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAliyunRouteEntryFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryAliyunVirtualRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryAliyunVirtualRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryAliyunVirtualRouterFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryApplianceVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryApplianceVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryApplianceVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalChassisAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalChassisAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryBaremetalHardwareInfo(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalHardwareInfoAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalHardwareInfoAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryBaremetalHostCfg(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalHostCfgAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalHostCfgAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryBaremetalPxeServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryCephPrimaryStoragePool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryCephPrimaryStoragePoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryCephPrimaryStoragePoolAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryClusterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryConnectionAccessPointFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConnectionAccessPointFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConnectionAccessPointFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryConnectionBetweenL3NetworkAndAliyunVSwitch(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConnectionBetweenL3NetworkAndAliyunVSwitchAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConnectionBetweenL3NetworkAndAliyunVSwitchAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryConsoleProxyAgentAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryDataCenterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDataCenterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDataCenterFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryDiskOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryEcsImageFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsImageFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsImageFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryEcsInstanceFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsInstanceFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsInstanceFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def queryEcsSecurityGroupRuleFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsSecurityGroupRuleFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryEcsVSwitchFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVSwitchFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVSwitchFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryEcsVpcFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEcsVpcFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEcsVpcFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEipAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryEmailMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEmailMediaAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEmailMediaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryEmailTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryEmailTriggerActionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryEmailTriggerActionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryFusionstorPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGCJobAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryGlobalConfigAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHostAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryHybridEipFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryHybridEipFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryHybridEipFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryIPSecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIPSecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIPSecConnectionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryIdentityZoneFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIdentityZoneFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIdentityZoneFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryImageStoreBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryInstanceOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryIpRangeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryL2VlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VlanNetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryL2VxlanNetwork(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryL2VxlanNetworkPool(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL2VxlanNetworkPoolAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL2VxlanNetworkPoolAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryLdapBinding(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapBindingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapBindingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLdapServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLoadBalancerListenerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryLocalStorageResourceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryLocalStorageResourceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryLocalStorageResourceRefAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryManagementNode(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryManagementNodeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryManagementNodeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryMediaAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryMediaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryMonitorTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryMonitorTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryMonitorTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryMonitorTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryMonitorTriggerActionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryMonitorTriggerActionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryNetworkServiceL3NetworkRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceL3NetworkRefAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryNetworkServiceProvider(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNetworkServiceProviderAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNetworkServiceProviderAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryNotification(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryNotificationSubscription(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryNotificationSubscriptionAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryNotificationSubscriptionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


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


    def queryPciDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPciDeviceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPciDeviceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryPciDeviceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPciDeviceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPciDeviceOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryPciDevicePciDeviceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryPciDevicePciDeviceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryPciDevicePciDeviceOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def querySchedulerJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerJobAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySchedulerJobAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def querySchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QuerySchedulerTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.QuerySchedulerTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def queryShareableVolumeVmInstanceRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryShareableVolumeVmInstanceRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryShareableVolumeVmInstanceRefAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def queryVRouterRouteEntry(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVRouterRouteEntryAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVRouterRouteEntryAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVRouterRouteTable(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVRouterRouteTableAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVRouterRouteTableAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVirtualBorderRouterFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualBorderRouterFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVirtualRouterVRouterRouteTableRef(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterVRouterRouteTableRefAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterVRouterRouteTableRefAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVirtualRouterVm(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVirtualRouterVmAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVirtualRouterVmAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVmNic(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVmNicInSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVmNicInSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVmNicInSecurityGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVniRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVniRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVniRangeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVolumeSnapshotTree(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVolumeSnapshotTreeAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVolumeSnapshotTreeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVpcIkeConfigFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVpcIkeConfigFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVpcIkeConfigFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVpcIpSecConfigFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVpcIpSecConfigFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVpcIpSecConfigFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVpcUserVpnGatewayFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVpcUserVpnGatewayFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVpcUserVpnGatewayFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVpcVpnConnectionFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVpcVpnConnectionFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVpcVpnConnectionFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVpcVpnGatewayFromLocal(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVpcVpnGatewayFromLocalAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVpcVpnGatewayFromLocalAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryVtep(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryVtepAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryVtepAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryWebhookAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def queryZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.QueryZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.QueryZoneAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        
        a.conditions = a.conditions.collect { it.toString() }


        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def rebootEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootEcsInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def rebootVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RebootVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RebootVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def reclaimSpaceFromImageStore(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReclaimSpaceFromImageStoreAction.class) Closure c) {
        def a = new org.zstack.sdk.ReclaimSpaceFromImageStoreAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def reconnectBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def reconnectConsoleProxyAgent(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectConsoleProxyAgentAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectConsoleProxyAgentAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def reconnectHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectHostAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def reconnectImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectImageStoreBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def reconnectPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def reconnectVirtualRouter(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReconnectVirtualRouterAction.class) Closure c) {
        def a = new org.zstack.sdk.ReconnectVirtualRouterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def recoverDataVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverDataVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverDataVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def recoverImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverImageAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverImageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def recoverVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoverVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoverVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def recoveryVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.RecoveryVirtualBorderRouterRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def refreshLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RefreshLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RefreshLoadBalancerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def reimageVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReimageVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ReimageVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def reloadLicense(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ReloadLicenseAction.class) Closure c) {
        def a = new org.zstack.sdk.ReloadLicenseAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def removeDnsFromL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveDnsFromL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveDnsFromL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def removeMonFromCephBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def removeMonFromCephPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromCephPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def removeMonFromFusionstorBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def removeMonFromFusionstorPrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveMonFromFusionstorPrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def removeSchedulerJobFromSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveSchedulerJobFromSchedulerTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveSchedulerJobFromSchedulerTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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


    def removeVmNicFromLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RemoveVmNicFromLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.RemoveVmNicFromLoadBalancerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def requestBaremetalConsoleAccess(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RequestBaremetalConsoleAccessAction.class) Closure c) {
        def a = new org.zstack.sdk.RequestBaremetalConsoleAccessAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def requestConsoleAccess(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RequestConsoleAccessAction.class) Closure c) {
        def a = new org.zstack.sdk.RequestConsoleAccessAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def resumeVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ResumeVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.ResumeVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def revertVolumeFromSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevertVolumeFromSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.RevertVolumeFromSnapshotAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def revokeResourceSharing(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.RevokeResourceSharingAction.class) Closure c) {
        def a = new org.zstack.sdk.RevokeResourceSharingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setImageQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetImageQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetImageQgaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setL3NetworkMtu(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetL3NetworkMtuAction.class) Closure c) {
        def a = new org.zstack.sdk.SetL3NetworkMtuAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setL3NetworkRouterInterfaceIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetL3NetworkRouterInterfaceIpAction.class) Closure c) {
        def a = new org.zstack.sdk.SetL3NetworkRouterInterfaceIpAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setNicQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetNicQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetNicQosAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVipQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVipQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVipQosAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmBootOrder(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmBootOrderAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmBootOrderAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmConsolePassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmConsolePasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmConsolePasswordAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmHostname(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmHostnameAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmHostnameAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmInstanceHaLevel(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmInstanceHaLevelAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmInstanceHaLevelAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmMonitorNumber(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmMonitorNumberAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmMonitorNumberAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmQga(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmQgaAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmQgaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmRDP(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmRDPAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmRDPAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmSshKey(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmSshKeyAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmSshKeyAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmStaticIp(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmStaticIpAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmStaticIpAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVmUsbRedirect(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVmUsbRedirectAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVmUsbRedirectAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def setVolumeQos(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SetVolumeQosAction.class) Closure c) {
        def a = new org.zstack.sdk.SetVolumeQosAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def shareResource(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ShareResourceAction.class) Closure c) {
        def a = new org.zstack.sdk.ShareResourceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def startBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.StartBaremetalPxeServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def startEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartEcsInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def startVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StartVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StartVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def stopBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.StopBaremetalPxeServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def stopEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopEcsInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def stopVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.StopVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.StopVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncAliyunRouteEntryFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncAliyunRouteEntryFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncAliyunRouteEntryFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncAliyunVirtualRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncAliyunVirtualRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncAliyunVirtualRouterFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncConnectionAccessPointFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncConnectionAccessPointFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncConnectionAccessPointFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncDataCenterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncDataCenterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncDataCenterFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncEcsImageFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsImageFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsImageFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncEcsInstanceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsInstanceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsInstanceFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncEcsSecurityGroupFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsSecurityGroupFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncEcsSecurityGroupRuleFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsSecurityGroupRuleFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsSecurityGroupRuleFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncEcsVSwitchFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVSwitchFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVSwitchFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncEcsVpcFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncEcsVpcFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncEcsVpcFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncHybridEipFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncHybridEipFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncHybridEipFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncIdentityFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncIdentityFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncIdentityFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncImageSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncImageSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncImageSizeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncPrimaryStorageCapacity(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncPrimaryStorageCapacityAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncPrimaryStorageCapacityAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncRouterInterfaceFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncRouterInterfaceFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncRouterInterfaceFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncVirtualBorderRouterFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVirtualBorderRouterFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncVolumeSize(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVolumeSizeAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVolumeSizeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncVpcUserVpnGatewayFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVpcUserVpnGatewayFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVpcUserVpnGatewayFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncVpcVpnConnectionFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVpcVpnConnectionFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVpcVpnConnectionFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def syncVpcVpnGatewayFromRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.SyncVpcVpnGatewayFromRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.SyncVpcVpnGatewayFromRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def terminateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.TerminateVirtualBorderRouterRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def triggerGCJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.TriggerGCJobAction.class) Closure c) {
        def a = new org.zstack.sdk.TriggerGCJobAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateAccount(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAccountAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateAccountAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateAliyunKeySecret(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateAliyunKeySecretAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateAliyunKeySecretAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateBaremetalChassis(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalChassisAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBaremetalChassisAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateBaremetalPxeServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateBaremetalPxeServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateBaremetalPxeServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateCephBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephBackupStorageMonAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateCephPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateCephPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateCephPrimaryStorageMonAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateCluster(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateClusterAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateClusterAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateConnectionBetweenL3NetWorkAndAliyunVSwitch(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateConnectionBetweenL3NetWorkAndAliyunVSwitchAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateConnectionBetweenL3NetWorkAndAliyunVSwitchAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateDiskOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateDiskOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateDiskOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateEcsInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEcsInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEcsInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateEcsInstanceVncPassword(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEcsInstanceVncPasswordAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEcsInstanceVncPasswordAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateEip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateEmailMedia(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEmailMediaAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEmailMediaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateEmailMonitorTriggerAction(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateEmailMonitorTriggerActionAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateEmailMonitorTriggerActionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateFusionstorBackupStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorBackupStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorBackupStorageMonAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateFusionstorPrimaryStorageMon(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateFusionstorPrimaryStorageMonAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateGlobalConfig(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateGlobalConfigAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateGlobalConfigAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateHostIommuState(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateHostIommuStateAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateHostIommuStateAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateIPsecConnection(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIPsecConnectionAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateIPsecConnectionAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateImage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateImageStoreBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateImageStoreBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateImageStoreBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateInstanceOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateInstanceOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateInstanceOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateIpRange(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateIpRangeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateIpRangeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateKVMHost(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateKVMHostAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateKVMHostAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateL2Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL2NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL2NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateL3Network(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateL3NetworkAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateL3NetworkAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateLdapServer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLdapServerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateLdapServerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateLoadBalancer(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLoadBalancerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateLoadBalancerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateLoadBalancerListener(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateLoadBalancerListenerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateLoadBalancerListenerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateMonitorTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateMonitorTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateMonitorTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateNotificationsStatus(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateNotificationsStatusAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateNotificationsStatusAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updatePciDevice(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePciDeviceAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePciDeviceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updatePortForwardingRule(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePortForwardingRuleAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePortForwardingRuleAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updatePrimaryStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdatePrimaryStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdatePrimaryStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateQuota(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateQuotaAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateQuotaAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateRouteInterfaceRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateRouteInterfaceRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateRouteInterfaceRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateSchedulerJob(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerJobAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSchedulerJobAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateSchedulerTrigger(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSchedulerTriggerAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSchedulerTriggerAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateSecurityGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSecurityGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSecurityGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateSftpBackupStorage(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSftpBackupStorageAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSftpBackupStorageAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateSystemTag(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateSystemTagAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateSystemTagAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateUser(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateUserGroup(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateUserGroupAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateUserGroupAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateVip(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVipAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVipAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateVirtualBorderRouterRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualBorderRouterRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateVirtualRouterOffering(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVirtualRouterOfferingAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVirtualRouterOfferingAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateVmInstance(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVmInstanceAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVmInstanceAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateVolume(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateVolumeSnapshot(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVolumeSnapshotAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVolumeSnapshotAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateVpcVpnConnectionRemote(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateVpcVpnConnectionRemoteAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateVpcVpnConnectionRemoteAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateWebhook(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateWebhookAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateWebhookAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def updateZone(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.UpdateZoneAction.class) Closure c) {
        def a = new org.zstack.sdk.UpdateZoneAction()
        a.sessionId = Test.currentEnvSpec?.session?.uuid
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
            def path = tracker.getApiPath()
            if (!path.isEmpty()) {
                Test.apiPaths[a.class.name] = path.join(" --->\n")
            }
        
            return out
        } else {
            return errorOut(a.call())
        }
    }


    def validateSession(@DelegatesTo(strategy = Closure.OWNER_FIRST, value = org.zstack.sdk.ValidateSessionAction.class) Closure c) {
        def a = new org.zstack.sdk.ValidateSessionAction()
        
        c.resolveStrategy = Closure.OWNER_FIRST
        c.delegate = a
        c()
        

        if (System.getProperty("apipath") != null) {
            if (a.apiId == null) {
                a.apiId = Platform.uuid
            }
    
            def tracker = new ApiPathTracker(a.apiId)
            def out = errorOut(a.call())
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
