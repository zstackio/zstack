package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.identity.AccountResourceRefVO_
import org.zstack.header.identity.AccountType
import org.zstack.header.identity.IdentityErrors
import org.zstack.header.errorcode.SysErrors
import org.zstack.core.Platform
import org.zstack.header.identity.SharedResourceVO
import org.zstack.header.identity.SharedResourceVO_
import org.zstack.network.service.lb.LoadBalancerBackendServerState
import org.zstack.network.service.lb.LoadBalancerListenerServerGroupVmNicRefVO
import org.zstack.network.service.lb.LoadBalancerListenerServerGroupVmNicRefVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend


class LoadBalancerServerGroupLifeCycleCase extends SubCase{
    DatabaseFacade dbf
    EnvSpec env
    LoadBalancerServerGroupInventory servergroup1
    LoadBalancerServerGroupInventory servergroup2
    LoadBalancerServerGroupInventory servergroup3
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
                    guestOsType = VirtualRouterConstant.X86_VPC_EULER_GUEST_OS_TYPE
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                lb {
                    name = "lb"
                    useVip("pubL3")

                    listener {
                        name = "listener-22"
                        protocol = "tcp"
                        loadBalancerPort = 22
                        instancePort = 22
                    }

                    listener {
                        name = "listener-33"
                        protocol = "tcp"
                        loadBalancerPort = 33
                        instancePort = 33
                    }

                    listener {
                        name = "listener-44"
                        protocol = "tcp"
                        loadBalancerPort = 44
                        instancePort = 44
                    }
                }
            }

            vm {
                name = "vm-1"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-2"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-3"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-4"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm-5"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            createServerGroup()
            TestUpdateLoadBalancerServerGroup()
            TestServerGroupWithBackendServer()
            testBackendReadPermissions()
            TestGetServerGroupBackendServer()
            TestDeleteLoadBalancerServerGroup()
        }
    }

    void testBackendReadPermissions() {
        def listener = env.inventoryByName("listener-22") as LoadBalancerListenerInventory
        def resources = [listener.uuid, servergroup2.uuid]
        def ownerRef = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, servergroup2.uuid).find()
        def listenerOwnerRef = Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.resourceUuid, listener.uuid).find()
        assert ownerRef != null && listenerOwnerRef != null
        assert ownerRef.ownerAccountUuid == listenerOwnerRef.ownerAccountUuid

        def adminMembers = getLoadBalancerListenerBackendServers {
            listenerUuid = listener.uuid
            serverGroupUuid = servergroup2.uuid
        } as List<LoadBalancerListenerBackendServerInventory>
        assert adminMembers.size() == 1
        assert adminMembers[0].vmNicUuid != null

        def account = createAccount {
            name = "backend-reader"
            password = "password"
        } as AccountInventory
        def session = logInByAccount {
            accountName = account.name
            password = "password"
        } as SessionInventory

        def checkAccess = { String phase, int listenerCount, int groupCount, boolean allowed ->
            assert queryLoadBalancerListener {
                conditions = ["uuid=${listener.uuid}".toString()]
                sessionId = session.uuid
            }.size() == listenerCount
            assert queryLoadBalancerServerGroup {
                conditions = ["uuid=${servergroup2.uuid}".toString()]
                sessionId = session.uuid
            }.size() == groupCount
            def action = new GetLoadBalancerListenerBackendServersAction(
                    listenerUuid: listener.uuid, serverGroupUuid: servergroup2.uuid,
                    sessionId: session.uuid)
            def result = action.call()
            if (allowed) {
                assert result.error == null : result.error
                assert result.value.inventories.size() == adminMembers.size()
                assert result.value.inventories[0].vmNicUuid == adminMembers[0].vmNicUuid
                assert result.value.inventories[0].ipAddress == adminMembers[0].ipAddress
                assert result.value.inventories[0].weight == adminMembers[0].weight
                assert result.value.inventories[0].instancePort == adminMembers[0].instancePort
            } else {
                assert result.error != null : "GetBackend exposed members during ${phase}"
                assert result.error.code == Constants.HTTP_ERROR
                def body = JSONObjectUtil.toObject(result.error.details, Map.class)
                assert body.error.code == IdentityErrors.PERMISSION_DENIED.toString()
                assert body.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10195"
                assert !body.containsKey("inventories")
                assert result.value == null
            }
            println "Backend read permissions: phase=${phase}, allowed=${allowed}, error=${result.error?.code}"
        }

        try {
            assert account.type == AccountType.Normal.toString()
            assert ownerRef.ownerAccountUuid != account.uuid
            assert Q.New(SharedResourceVO.class).in(SharedResourceVO_.resourceUuid, resources).count() == 0
            checkAccess("unshared", 0, 0, false)
            shareResource {
                resourceUuids = [listener.uuid]
                accountUuids = [account.uuid]
            }
            checkAccess("listener-only", 1, 0, false)
            revokeResourceSharing {
                resourceUuids = [listener.uuid]
                accountUuids = [account.uuid]
            }
            shareResource {
                resourceUuids = [servergroup2.uuid]
                accountUuids = [account.uuid]
            }
            checkAccess("group-only", 0, 1, false)
            shareResource {
                resourceUuids = [listener.uuid]
                accountUuids = [account.uuid]
            }
            checkAccess("both-shared", 1, 1, true)
            revokeResourceSharing {
                resourceUuids = resources
                accountUuids = [account.uuid]
            }
            assert Q.New(SharedResourceVO.class).in(SharedResourceVO_.resourceUuid, resources).count() == 0
            checkAccess("revoked", 0, 0, false)
            shareResource {
                resourceUuids = resources
                toPublic = true
            }
            checkAccess("public", 1, 1, true)
            revokeResourceSharing {
                resourceUuids = resources
                toPublic = true
                all = true
            }
            checkAccess("public-revoked", 0, 0, false)
            resources.each { resource ->
                changeResourceOwner {
                    resourceUuid = resource
                    accountUuid = account.uuid
                }
            }
            checkAccess("owner", 1, 1, true)

            def absent = new GetLoadBalancerListenerBackendServersAction(
                    listenerUuid: Platform.getUuid(), serverGroupUuid: servergroup2.uuid,
                    sessionId: session.uuid).call()
            assert absent.error != null
            assert absent.error.code == Constants.HTTP_ERROR
            assert JSONObjectUtil.toObject(absent.error.details, Map.class).error.code ==
                    SysErrors.RESOURCE_NOT_FOUND.toString()
            assert absent.value == null

            def unattached = new GetLoadBalancerListenerBackendServersAction(
                    listenerUuid: listener.uuid, serverGroupUuid: servergroup1.uuid,
                    sessionId: env.session.uuid).call()
            assert unattached.error != null
            assert unattached.error.code == Constants.HTTP_ERROR
            assert JSONObjectUtil.toObject(unattached.error.details, Map.class).error.code ==
                    SysErrors.INVALID_ARGUMENT_ERROR.toString()
            assert unattached.value == null

            def otherVip = createVip {
                name = "other-lb-vip"
                l3NetworkUuid = (env.inventoryByName("pubL3") as L3NetworkInventory).uuid
            } as VipInventory
            try {
                def otherLb = createLoadBalancer {
                    name = "other-lb"
                    vipUuid = otherVip.uuid
                } as LoadBalancerInventory
                try {
                    def otherGroup = createLoadBalancerServerGroup {
                        name = "other-lb-group"
                        loadBalancerUuid = otherLb.uuid
                    } as LoadBalancerServerGroupInventory
                    def mismatched = new GetLoadBalancerListenerBackendServersAction(
                            listenerUuid: listener.uuid, serverGroupUuid: otherGroup.uuid,
                            sessionId: env.session.uuid).call()
                    assert mismatched.error != null
                    assert mismatched.error.code == Constants.HTTP_ERROR
                    assert JSONObjectUtil.toObject(mismatched.error.details, Map.class).error.code ==
                            SysErrors.INVALID_ARGUMENT_ERROR.toString()
                    assert mismatched.value == null
                } finally {
                    deleteLoadBalancer { uuid = otherLb.uuid }
                }
            } finally {
                deleteVip { uuid = otherVip.uuid }
            }
        } finally {
            revokeResourceSharing {
                resourceUuids = resources
                all = true
            }
            resources.each { resource ->
                changeResourceOwner {
                    resourceUuid = resource
                    accountUuid = ownerRef.ownerAccountUuid
                }
            }
            logOut { sessionUuid = session.uuid }
            deleteAccount { uuid = account.uuid }
        }
    }

    void createServerGroup(){
        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        servergroup1 = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "lb-group-1"
        }
        assert servergroup1.name == "lb-group-1"
        LoadBalancerServerGroupInventory servergroup = queryLoadBalancerServerGroup { conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.name == "lb-group-1"

        servergroup2 = createLoadBalancerServerGroup {
            loadBalancerUuid =  lb.uuid
            name = "lb-group-2"
        }
        assert servergroup2.name == "lb-group-2"

        servergroup3 = createLoadBalancerServerGroup {
            loadBalancerUuid =  lb.uuid
            name = "null-group"
        }


    }

    void TestUpdateLoadBalancerServerGroup(){
        updateLoadBalancerServerGroup {
            uuid = servergroup1.uuid
            name = "updated name"
        }
        LoadBalancerServerGroupInventory servergroup = queryLoadBalancerServerGroup { conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.name == "updated name"
    }

    void TestServerGroupWithBackendServer(){
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def lbl1 = env.inventoryByName("listener-22") as LoadBalancerListenerInventory

        VmInstanceInventory vm1 = queryVmInstance {conditions = ["name=vm-1"]} [0]
        VmNicInventory nic1 = vm1.vmNics.get(0)

        VmInstanceInventory vm2 = queryVmInstance {conditions = ["name=vm-2"]} [0]
        VmNicInventory nic2 = vm2.vmNics.get(0)

        VmInstanceInventory vm3 = queryVmInstance {conditions = ["name=vm-3"]} [0]
        VmNicInventory nic3 = vm3.vmNics.get(0)

        VmInstanceInventory vm4 = queryVmInstance {conditions = ["name=vm-4"]} [0]
        VmNicInventory nic4 = vm4.vmNics.get(0)

        /* shared load balancer can not add server group */
        expect(AssertionError.class) {
            addBackendServerToServerGroup {
                vmNics = [['uuid':nic1.uuid,'weight':'20']]
                servers  = [['ipAddress':"20.20.20.1",'weight':'30']]
                serverGroupUuid = servergroup1.uuid
            }
        }
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'10']]
            serverGroupUuid = servergroup1.uuid
        }
        LoadBalancerServerGroupInventory servergroup = queryLoadBalancerServerGroup{ conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.vmNicRefs.size() == 1
        assert servergroup.vmNicRefs.get(0).vmNicUuid == nic1.uuid
        assert servergroup.vmNicRefs[0].weight == 10

        /* shared load balancer can not change server ip  */
        expect(AssertionError.class) {
            changeLoadBalancerBackendServer {
                vmNics = [['uuid': nic1.uuid, 'weight': '40']]
                servers = [['ipAddress': "20.20.20.1", 'weight': '50']]
                serverGroupUuid = servergroup1.uuid
            }
        }

        changeLoadBalancerBackendServer {
            vmNics = [['uuid': nic1.uuid, 'weight': '40']]
            serverGroupUuid = servergroup1.uuid
        }

        changeLoadBalancerBackendServer {
            vmNics = [['uuid': nic1.uuid, 'weight': '40']]
            serverGroupUuid = servergroup1.uuid
        }

        expect(AssertionError.class) {
            changeLoadBalancerBackendServer {
                serverGroupUuid = servergroup1.uuid
            }
        }

        addBackendServerToServerGroup {
            vmNics = [['uuid':nic2.uuid,'weight':'20'],['uuid':nic3.uuid,'weight':'30']]
            serverGroupUuid = servergroup1.uuid
        }

        expect(AssertionError.class) {
            addBackendServerToServerGroup {
                vmNics = [['uuid':vm1.uuid,'weight':'20']]
                serverGroupUuid = servergroup1.uuid
            }
        }

        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
        }

        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'10'], ['uuid':nic4.uuid,'weight':'40']]
            serverGroupUuid = servergroup2.uuid
        }

        /* nic1, has been added to server group1 and server group2, so server group2 can not be added to listener1 */
        expect(AssertionError.class) {
            addServerGroupToLoadBalancerListener {
                listenerUuid = lbl1.uuid
                serverGroupUuid = servergroup2.uuid
            }
        }

        removeBackendServerFromServerGroup {
            serverGroupUuid = servergroup2.uuid
            vmNicUuids = [nic1.uuid]
        }

        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup2.uuid
        }
        /* nic1 has been added to server group1 which is attached to listener1, so nic1 can not be added to server group2 */
        expect(AssertionError.class) {
            addBackendServerToServerGroup {
                vmNics = [['uuid':nic1.uuid]]
                serverGroupUuid = servergroup2.uuid
            }
        }

        /* delete server group refresh backend */
        removeServerGroupFromLoadBalancerListener {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
        }

        servergroup = queryLoadBalancerServerGroup{ conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.vmNicRefs.size() == 3

        removeBackendServerFromServerGroup{
            vmNicUuids = [nic1.uuid]
            serverGroupUuid = servergroup1.uuid
        }
        servergroup = queryLoadBalancerServerGroup{ conditions = ["uuid=${servergroup1.uuid}".toString()]}[0]
        assert servergroup.vmNicRefs.size() == 2


        def lbl3 = env.inventoryByName("listener-44") as LoadBalancerListenerInventory
        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl3.uuid
            serverGroupUuid = servergroup3.uuid
        }
        // can remove servergroup which don not have vmnic and serverip
        removeServerGroupFromLoadBalancerListener {
            listenerUuid = lbl3.uuid
            serverGroupUuid = servergroup3.uuid
        }




    }

    void TestGetServerGroupBackendServer(){
        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        VmInstanceInventory vm2 = queryVmInstance {conditions = ["name=vm-2"]} [0]
        VmNicInventory nic2 = vm2.vmNics.get(0)
        VmInstanceInventory vm3 = queryVmInstance {conditions = ["name=vm-3"]} [0]
        VmNicInventory nic3 = vm3.vmNics.get(0)
        VmInstanceInventory vm4 = queryVmInstance {conditions = ["name=vm-4"]} [0]
        VmNicInventory nic4 = vm4.vmNics.get(0)

        def lbl1 = env.inventoryByName("listener-22") as LoadBalancerListenerInventory

        /* group2 is already attached to lbl1, attach group1 so both are covered */
        addServerGroupToLoadBalancerListener {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
        }

        /* listener view returns backends of all groups attached to the listener */
        def action = new org.zstack.sdk.GetLoadBalancerServerGroupBackendServerAction()
        action.listenerUuid = lbl1.uuid
        action.sessionId = org.zstack.testlib.Test.currentEnvSpec.session.uuid
        def ret = action.call()
        assert ret.error == null
        assert ret.value.total == 3
        assert ret.value.inventories.size() == 3
        assert ret.value.inventories.every { it.loadBalancerUuid == lb.uuid }
        assert ret.value.inventories.every { it.listenerUuid == lbl1.uuid }
        assert ret.value.inventories.every { it.state == "Enabled" }
        assert ret.value.inventories.every { it.runtimeStatus == "Active" }
        assert ret.value.inventories.count { it.serverGroupUuid == servergroup1.uuid } == 2
        assert ret.value.inventories.count { it.serverGroupUuid == servergroup2.uuid } == 1
        assert ret.value.inventories.collect { it.uuid }.unique().size() == 3
        assert ret.value.inventories.every { it.uuid.length() == 32 }
        assert ret.value.inventories.findAll { it.serverGroupUuid == servergroup1.uuid }.every { it.serverGroupName == "updated name" }
        assert ret.value.inventories.findAll { it.serverGroupUuid == servergroup2.uuid }.every { it.serverGroupName == "lb-group-2" }
        assert ret.value.inventories.find { it.vmNicUuid == nic2.uuid }.serverName == "vm-2"
        assert ret.value.inventories.find { it.vmNicUuid == nic4.uuid }.serverName == "vm-4"

        /* serverGroupUuid filters rows down to one group */
        def invs = getLoadBalancerServerGroupBackendServer {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
        }
        assert invs.size() == 2 && invs.every { it.serverGroupUuid == servergroup1.uuid }

        /* search by name across groups */
        invs = getLoadBalancerServerGroupBackendServer {
            listenerUuid = lbl1.uuid
            name = "vm-"
        }
        assert invs.size() == 3

        invs = getLoadBalancerServerGroupBackendServer {
            listenerUuid = lbl1.uuid
            name = "vm-4"
        }
        assert invs.size() == 1 && invs[0].serverName == "vm-4"

        /* sort by server name asc */
        invs = getLoadBalancerServerGroupBackendServer {
            listenerUuid = lbl1.uuid
            sortBy = "serverName"
            sortDirection = "asc"
        }
        assert invs.size() == 3
        assert invs[0].serverName == "vm-2" && invs[1].serverName == "vm-3" && invs[2].serverName == "vm-4"

        /* pagination: total counts all matches, inventories are sliced */
        action = new org.zstack.sdk.GetLoadBalancerServerGroupBackendServerAction()
        action.listenerUuid = lbl1.uuid
        action.sortBy = "serverName"
        action.sortDirection = "asc"
        action.limit = 2
        action.start = 0
        action.sessionId = org.zstack.testlib.Test.currentEnvSpec.session.uuid
        ret = action.call()
        assert ret.error == null
        assert ret.value.total == 3
        assert ret.value.inventories.size() == 2
        assert ret.value.inventories[0].serverName == "vm-2"
        assert ret.value.inventories[1].serverName == "vm-3"

        action.start = 2
        ret = action.call()
        assert ret.error == null
        assert ret.value.total == 3
        assert ret.value.inventories.size() == 1
        assert ret.value.inventories[0].serverName == "vm-4"

        /* serverGroupUuid filter works together with pagination */
        action.serverGroupUuid = servergroup1.uuid
        action.start = 0
        ret = action.call()
        assert ret.error == null
        assert ret.value.total == 2
        assert ret.value.inventories.size() == 2

        /* disable vm-2 on listener lbl1 */
        changeLoadBalancerListenerBackendServerState {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
            vmNicUuids = [nic2.uuid]
            state = "Disabled"
        }

        // A legacy Enabled row must still mean Disabled in both query APIs.
        SQL.New(LoadBalancerListenerServerGroupVmNicRefVO.class)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.listenerUuid, lbl1.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.serverGroupUuid, servergroup1.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.vmNicUuid, nic2.uuid)
                .set(LoadBalancerListenerServerGroupVmNicRefVO_.state, LoadBalancerBackendServerState.Enabled)
                .update()
        def listenerBackends = getLoadBalancerListenerBackendServers {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
        }
        assert listenerBackends.find { it.vmNicUuid == nic2.uuid }.state == "Disabled"
        assert listenerBackends.find { it.vmNicUuid == nic2.uuid }.healthStatus == "Unchecked"

        invs = getLoadBalancerServerGroupBackendServer {
            listenerUuid = lbl1.uuid
            state = "Disabled"
        }
        assert invs.size() == 1 && invs[0].serverName == "vm-2"

        invs = getLoadBalancerServerGroupBackendServer {
            listenerUuid = lbl1.uuid
            state = "Enabled"
        }
        assert invs.size() == 2

        /* runtime status still reflects vm nic state while state is the enable/disable state */
        invs = getLoadBalancerServerGroupBackendServer {
            listenerUuid = lbl1.uuid
        }
        assert invs.find { it.serverName == "vm-2" }.state == "Disabled"
        assert invs.find { it.serverName == "vm-2" }.runtimeStatus == "Active"
        assert invs.find { it.serverName == "vm-3" }.state == "Enabled"

        /* name search and state filter work together */
        invs = getLoadBalancerServerGroupBackendServer {
            listenerUuid = lbl1.uuid
            name = "vm-3"
            state = "Enabled"
        }
        assert invs.size() == 1 && invs[0].serverName == "vm-3"

        /* re-enable vm-2 */
        changeLoadBalancerListenerBackendServerState {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
            vmNicUuids = [nic2.uuid]
            state = "Enabled"
        }

        invs = getLoadBalancerServerGroupBackendServer {
            listenerUuid = lbl1.uuid
        }
        assert invs.every { it.state == "Enabled" }
        assert !Q.New(LoadBalancerListenerServerGroupVmNicRefVO.class)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.listenerUuid, lbl1.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.serverGroupUuid, servergroup1.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.vmNicUuid, nic2.uuid)
                .isExists()
        listenerBackends = getLoadBalancerListenerBackendServers {
            listenerUuid = lbl1.uuid
            serverGroupUuid = servergroup1.uuid
        }
        assert listenerBackends.find { it.vmNicUuid == nic2.uuid }.state == "Enabled"
        assert listenerBackends.find { it.vmNicUuid == nic2.uuid }.healthStatus == "Unknown"
    }


    void TestDeleteLoadBalancerServerGroup(){
        deleteLoadBalancerServerGroup{
            uuid = servergroup1.uuid
        }

        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        expect(AssertionError.class) {
            deleteLoadBalancerServerGroup {
                uuid = lb.serverGroupUuid
            }
        }

        def lbl2 = env.inventoryByName("listener-33") as LoadBalancerListenerInventory

        VmInstanceInventory vm5 = queryVmInstance {conditions = ["name=vm-5"]} [0]
        VmNicInventory nic5 = vm5.vmNics.get(0)

        lbl2 =  addVmNicToLoadBalancer {
            vmNicUuids = [nic5.uuid]
            listenerUuid = lbl2.uuid
        }

        assert lbl2.vmNicRefs[0].vmNicUuid == nic5.uuid


        removeVmNicFromLoadBalancer {
            vmNicUuids = [nic5.uuid]
            listenerUuid = lbl2.uuid
        }
        lbl2 = queryLoadBalancerListener { conditions = ["uuid=${lbl2.uuid}".toString()]}[0]
        assert lbl2.vmNicRefs.size()==0

        deleteLoadBalancerServerGroup {
            uuid = lbl2.serverGroupUuid
        }

    }


    @Override
    void clean() {
        env.delete()
    }


}
