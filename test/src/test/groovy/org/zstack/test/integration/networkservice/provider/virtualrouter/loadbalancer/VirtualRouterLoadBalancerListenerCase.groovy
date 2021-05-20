package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.acl.AccessControlListEntryVO
import org.zstack.header.acl.AccessControlListEntryVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerAclStatus
import org.zstack.network.service.lb.LoadBalancerAclType
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerListenerACLRefVO
import org.zstack.network.service.lb.LoadBalancerSystemTags
import org.zstack.network.service.lb.LoadBalancerListenerACLRefVO_
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * @author: zhanyong.miao
 * @date: 2020-02-28
 * */
class VirtualRouterLoadBalancerListenerCase extends SubCase{
    DatabaseFacade dbf
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {

            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "vm2"
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
            testLoadBalancerHealthCheckCase()
            testLoadBalancerWrrCase()
            testLoadBalancerAclCase()
            testOperateLBRedirectRuleCase()
            testOperateLBRedirectAclCase()
        }
    }

    private void testLoadBalancerAclCase() {
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def _name = "test6"

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        /*acl default value: disable, black*/
        CreateLoadBalancerListenerAction listenerAction = new CreateLoadBalancerListenerAction()
        listenerAction.loadBalancerUuid = load.uuid
        listenerAction.name = _name
        listenerAction.loadBalancerPort = 66
        listenerAction.instancePort = 66
        listenerAction.protocol = "tcp"
        listenerAction.sessionId = adminSession()

        CreateLoadBalancerListenerAction.Result lblRes = listenerAction.call()
        assert lblRes.error == null

        String aclStatus = LoadBalancerSystemTags.BALANCER_ACL.getTokenByResourceUuid(lblRes.value.inventory.uuid, LoadBalancerSystemTags.BALANCER_ACL_TOKEN);
        assert aclStatus.equals(LoadBalancerAclStatus.disable.toString())
        assert cmd == null

        addVmNicToLoadBalancer {
            vmNicUuids = [vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid]
            listenerUuid = lblRes.value.inventory.uuid
        }
        assert cmd.getLbs().get(0).parameters.contains("accessControlStatus::disable")

        /*change acl status enable*/
        ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction()
        action.uuid  = lblRes.value.inventory.uuid
        action.aclStatus = LoadBalancerAclStatus.enable.toString()
        action.sessionId = adminSession()
        ChangeLoadBalancerListenerAction.Result res = action.call()
        assert res.error == null
        aclStatus = LoadBalancerSystemTags.BALANCER_ACL.getTokenByResourceUuid(lblRes.value.inventory.uuid, LoadBalancerSystemTags.BALANCER_ACL_TOKEN);
        assert aclStatus.equals(LoadBalancerAclStatus.enable.toString())
        assert cmd.getLbs().find {l -> l.listenerUuid == lblRes.value.inventory.uuid}.parameters.contains("accessControlStatus::enable")
        assert cmd.getLbs().find {l -> l.listenerUuid == lblRes.value.inventory.uuid}.parameters.contains("aclEntry::")

        AccessControlListInventory acl = createAccessControlList {
            name = "acl1"
        }

        AccessControlListInventory acl6 = createAccessControlList {
            name = "acl6"
            ipVersion = 6
        }

        AccessControlListInventory acl2 = createAccessControlList {
            name = "acl2"
            ipVersion = 4
        }

        addAccessControlListEntry {
            aclUuid = acl.uuid
            entries = "192.168.0.1,192.168.1.0/24"
        }

        addAccessControlListEntry {
            aclUuid = acl2.uuid
            entries = "192.168.0.1,192.168.1.0/24"
        }

        /*duplicate ip*/
        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListEntry {
                aclUuid = acl.uuid
                entries = "192.168.0.1,192.168.20.0/24"
            }
        }

        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListEntry {
                aclUuid = acl.uuid
                entries = "192.168.3.3,192.168.3.0/24"
            }
        }

        addAccessControlListToLoadBalancer {
            aclUuids = [acl.uuid]
            aclType = LoadBalancerAclType.black.toString()
            listenerUuid = lblRes.value.inventory.uuid
        }
        assert cmd.getLbs().find {l -> l.listenerUuid == lblRes.value.inventory.uuid}.parameters.contains("aclEntry::192.168.0.1,192.168.1.0/24")

        /*ip version verify*/
        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListToLoadBalancer {
                aclUuids = [acl6.uuid]
                aclType = LoadBalancerAclType.black.toString()
                listenerUuid = lblRes.value.inventory.uuid
            }
        }

        expect( [ApiException.class, AssertionError.class]) {
            createLoadBalancerListener {
                loadBalancerUuid = load.uuid
                name = "77"
                loadBalancerPort = 77
                instancePort = 77
                protocol = "tcp"
                aclStatus = "enable"
                aclUuids = [acl6.uuid]
            }
        }

        cmd = null
        AccessControlListEntryInventory entry = addAccessControlListEntry {
            aclUuid = acl.uuid
            entries = "192.168.0.2,192.168.2.0/24"
        }
        retryInSecs {
            assert cmd != null
        }
        cmd = null
        removeAccessControlListEntry {
            uuid = entry.uuid
            aclUuid = acl.uuid
        }
        retryInSecs {
            assert cmd.getLbs().find {l -> l.listenerUuid == lblRes.value.inventory.uuid}.parameters.contains("aclEntry::192.168.0.1,192.168.1.0/24")
        }

        /*can't add the acl overlap entry into same lbl */
        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListToLoadBalancer {
                aclUuids = [acl2.uuid]
                aclType = LoadBalancerAclType.black.toString()
                listenerUuid = lblRes.value.inventory.uuid
            }
        }


        LoadBalancerListenerInventory lbl2 = createLoadBalancerListener {
            loadBalancerUuid = load.uuid
            name = "77"
            loadBalancerPort = 77
            instancePort = 77
            protocol = "tcp"
            aclUuids = [acl.uuid]
        }
        LoadBalancerListenerACLRefInventory ref = lbl2.aclRefs.get(0);
        assert ref.aclUuid == acl.uuid
        assert ref.type == LoadBalancerAclType.black.toString()

        /*delete acl being used by lbl*/
        expect( [ApiException.class, AssertionError.class] ) {
            deleteAccessControlList {
                uuid = acl.uuid
            }
        }

        removeAccessControlListFromLoadBalancer {
            aclUuids =[acl.uuid]
            listenerUuid = lblRes.value.inventory.uuid
        }

        expect( [ApiException.class, AssertionError.class] ) {
            deleteAccessControlList {
                uuid = acl.uuid
            }
        }

        deleteLoadBalancerListener {
            uuid = lbl2.uuid
        }


        deleteAccessControlList {
            uuid = acl.uuid
        }
        deleteAccessControlList {
            uuid = acl2.uuid
        }
        deleteAccessControlList {
            uuid = acl6.uuid
        }
    }

    private void testOperateLBRedirectRuleCase() {

        AccessControlListInventory acl = createAccessControlList {
            name = "redirect-acl"
        }
        /*redirect operate*/
        //add redirect rule
         AccessControlListEntryInventory redirectRule = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/test"
            aclUuid = acl.uuid
        }

        AccessControlListEntryInventory redirectRule2 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = ""
            url = "/test"
            aclUuid = acl.uuid
        }

        AccessControlListEntryInventory redirectRule3 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "*.zstack.io"
            url = ""
            aclUuid = acl.uuid
        }

        expectError {
            addAccessControlListRedirectRule {
                name = "redirect rule"
                domain = ""
                url = ""
                aclUuid = acl.uuid
            }
        }

        AccessControlListEntryVO rlVO = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.uuid, redirectRule.uuid).find()
        assert rlVO != null
        assert rlVO.name == redirectRule.name
        assert rlVO.getType() == "RedirectRule" && rlVO.getType() == redirectRule.type
        assert rlVO.getMatchMethod() == "DomainAndUrl"
        assert rlVO.getCriterion() == "AccurateMatch"
        assert rlVO.getDomain() == "zstack.io"
        assert rlVO.getUrl() == "/test"

        rlVO = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.uuid, redirectRule2.uuid).find()
        assert rlVO != null
        assert rlVO.name == redirectRule2.name
        assert rlVO.getType() == "RedirectRule" && rlVO.getType() == redirectRule2.type
        assert rlVO.getCriterion() == null
        assert rlVO.getMatchMethod() == "Url"


        rlVO = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.uuid, redirectRule3.uuid).find()
        assert rlVO != null
        assert rlVO.name == redirectRule.name
        assert rlVO.getType() == "RedirectRule" && rlVO.getType() == redirectRule3.type
        assert rlVO.getCriterion() == "WildcardMatch"
        assert rlVO.getMatchMethod() == "Domain"

        //query acl
        List<AccessControlListInventory> aclList = queryAccessControlList {
            conditions = ["uuid=${acl.uuid}"]
        }
        assert aclList.get(0).entries.size() == 3
        assert aclList.get(0).entries.get(0).type == "RedirectRule"

        //change description

        //constraint: cannot add redirect rule and ip entry
        expectError {
            addAccessControlListEntry {
                aclUuid = acl.uuid
                entries = "192.168.0.1,192.168.1.0/24"
            }
        }

        //constraint: acl cannot add the exist redirect rule
        expectError {
            addAccessControlListRedirectRule {
                name = "redirect rule"
                domain = "zstack.io"
                url = "/test"
                aclUuid = acl.uuid
            }
        }

        //delete ACL will clean all redirect rule
        deleteAccessControlList {
            uuid = acl.uuid
        }
        rlVO = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.aclUuid, acl.uuid).find()
        assert rlVO == null
    }

    private void testOperateLBRedirectAclCase() {
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def _name = "test6"

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        /*acl default value: disable, black*/
        CreateLoadBalancerListenerAction listenerAction = new CreateLoadBalancerListenerAction()
        listenerAction.loadBalancerUuid = load.uuid
        listenerAction.name = _name
        listenerAction.loadBalancerPort = 8900
        listenerAction.instancePort = 8900
        listenerAction.protocol = "tcp"
        listenerAction.sessionId = adminSession()

        CreateLoadBalancerListenerAction.Result lblRes = listenerAction.call()
        assert lblRes.error == null

        AccessControlListInventory acl = createAccessControlList {
            name = "redirect-acl"
        }
        /*redirect operate*/
        //add redirect rule
        AccessControlListEntryInventory redirectRule = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/test"
            aclUuid = acl.uuid
        }

        AccessControlListEntryInventory redirectRule2 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = ""
            url = "/test"
            aclUuid = acl.uuid
        }

        /*add acl to server group*/
        //constraint: cant add redirect rule to tcp listener
        expectError {
            addAccessControlListToLoadBalancer {
                aclUuids = [acl.uuid]
                listenerUuid = lblRes.value.inventory.uuid
                aclType = "redirect"
            }
        }

        //constraint: cant add same rule to listener
        /*acl default value: disable, black*/
        listenerAction = new CreateLoadBalancerListenerAction()
        listenerAction.loadBalancerUuid = load.uuid
        listenerAction.name = _name
        listenerAction.loadBalancerPort = 8955
        listenerAction.instancePort = 8955
        listenerAction.protocol = "http"
        listenerAction.sessionId = adminSession()

        lblRes = listenerAction.call()
        assert lblRes.error == null

        //add acl to listener
        addAccessControlListToLoadBalancer {
            aclUuids = [acl.uuid]
            listenerUuid = lblRes.value.inventory.uuid
            aclType = "redirect"
        }
        List<LoadBalancerListenerACLRefVO> refVOList = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.listenerUuid, lblRes.value.inventory.uuid).list()
        assert refVOList.size() == 1
        assert refVOList.get(0).aclUuid == acl.uuid

        //add acl to server group
        List<LoadBalancerServerGroupInventory> serverGroupInventory = queryLoadBalancerServerGroup {
            conditions = ["loadBalancerUuid=${load.uuid}"]
        }
        addServerGroupToLoadBalancerListener {
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuid = serverGroupInventory.get(0).uuid
        }
        addAccessControlListToLoadBalancer {
            aclUuids = [acl.uuid]
            listenerUuid = lblRes.value.inventory.uuid
            aclType = "redirect"
            serverGroupUuids = [serverGroupInventory.get(0).uuid]
        }

        refVOList = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.listenerUuid, lblRes.value.inventory.uuid).list()
        assert refVOList.size() == 2
        refVOList = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.serverGroupUuid, serverGroupInventory.get(0).uuid).eq(LoadBalancerListenerACLRefVO_.listenerUuid, lblRes.value.inventory.uuid).list()
        assert refVOList.size() == 1

        AccessControlListInventory acl2 = createAccessControlList {
            name = "redirect-acl"
        }
        AccessControlListEntryInventory acl2_redirectRule = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/test"
            aclUuid = acl2.uuid
        }

        //constraint: acl has no redirect rule cannot attach to server group
        AccessControlListInventory acl3 = createAccessControlList {
            name = "redirect-acl-3"
        }
        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListToLoadBalancer {
                aclUuids = [acl3.uuid]
                listenerUuid = lblRes.value.inventory.uuid
                aclType = "redirect"
                serverGroupUuids = [serverGroupInventory.uuid]
            }
        }

        //constraint: cannot add acl which redirect rule listener had
        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListToLoadBalancer {
                aclUuids = [acl2.uuid]
                listenerUuid = lblRes.value.inventory.uuid
                aclType = "redirect"
            }
        }

        //constraint: cannot add servergroup which not belong to listener
        LoadBalancerServerGroupInventory groupNotAttachListener = createLoadBalancerServerGroup {
            loadBalancerUuid =  load.uuid
            name = "lb-not-attach-to-listener"
        }

        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListToLoadBalancer {
                aclUuids = [acl.uuid]
                listenerUuid = lblRes.value.inventory.uuid
                aclType = "redirect"
                serverGroupUuids = [groupNotAttachListener.uuid]
            }
        }

        deleteLoadBalancer {
            uuid = lblRes.value.inventory.loadBalancerUuid
        }
        assert Q.New(LoadBalancerListenerACLRefVO.class).count() == 0


        //delete acl
        deleteAccessControlList {
            uuid = acl.uuid
        }
        deleteAccessControlList {
            uuid = acl2.uuid
        }
        deleteAccessControlList {
            uuid = acl3.uuid
        }
    }

    private void testLoadBalancerHealthCheckCase() {
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def _name = "test5"

        CreateLoadBalancerListenerAction listenerAction = new CreateLoadBalancerListenerAction()
        listenerAction.loadBalancerUuid = load.uuid
        listenerAction.name = _name
        listenerAction.loadBalancerPort = 55
        listenerAction.instancePort = 55
        listenerAction.protocol = "tcp"
        listenerAction.healthCheckProtocol = "http"
        listenerAction.sessionId = adminSession()

        CreateLoadBalancerListenerAction.Result lblRes = listenerAction.call()
        assert lblRes.error != null

        listenerAction.healthCheckURI = "/health.html"
        lblRes = listenerAction.call()
        assert lblRes.error == null

        List<Map<String, String>> tokens = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);

        for (Map<String, String>  token: tokens) {
            assert token.get(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN) == "HEAD:/health.html:http_2xx"
        }

        ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction()
        action.uuid  = lblRes.value.inventory.uuid
        action.healthCheckURI = "/abcd.html"
        action.healthCheckMethod = "GET"
        action.healthCheckProtocol = "http"
        action.sessionId = adminSession()
        ChangeLoadBalancerListenerAction.Result res = action.call()
        assert res.error == null
        tokens = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);

        for (Map<String, String>  token: tokens) {
            assert token.get(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN) == "GET:/abcd.html:http_2xx"
        }

        action.healthCheckProtocol = "tcp"
        res = action.call()
        assert res.error == null
        tokens = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);
        assert tokens == null || tokens.isEmpty()

        action.healthCheckProtocol = "http"
        action.healthCheckMethod = null
        action.healthCheckURI = null
        res = action.call()
        assert res.error != null

        action.healthCheckURI = "/abc.html"
        action.healthCheckProtocol = "http"
        res = action.call()
        assert res.error == null
        tokens = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);

        for (Map<String, String>  token: tokens) {
            assert token.get(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN) == "HEAD:/abc.html:http_2xx"
        }

        deleteLoadBalancerListener {
            uuid = lblRes.value.inventory.uuid
        }

        tokens = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);
        assert tokens == null || tokens.isEmpty()
    }


    void testLoadBalancerWrrCase() {
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def _name = "test2"

        CreateLoadBalancerListenerAction listenerAction = new CreateLoadBalancerListenerAction()
        listenerAction.loadBalancerUuid = load.uuid
        listenerAction.name = _name
        listenerAction.loadBalancerPort = 44
        listenerAction.instancePort = 44
        listenerAction.protocol = "tcp"
        listenerAction.systemTags = ["balancerAlgorithm::weightroundrobin"]
        listenerAction.sessionId = adminSession()

        CreateLoadBalancerListenerAction.Result lblRes = listenerAction.call()
        assert lblRes.error == null

        List<String> nicUuids = [vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid, vm2.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid]
        addVmNicToLoadBalancer {
            vmNicUuids = nicUuids
            listenerUuid = lblRes.value.inventory.uuid
        }

        /* weight has been remove to server group
        List<Map<String, String>> tokens = LoadBalancerSystemTags.BALANCER_WEIGHT.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);

        nicUuids.forEach { it ->
            List<Map<String, String>> ts = tokens.stream().filter { Map<String, String> token -> it.equals(token.get(LoadBalancerSystemTags.BALANCER_NIC_TOKEN)) }.collect(Collectors.toList()) as List
            assert !ts.isEmpty()
            assert ts.get(0).get(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN) == LoadBalancerConstants.BALANCER_WEIGHT_default.toString()
        }*/

        String weight = "balancerWeight::" + vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid + "::20"
        ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction()
        action.uuid  = lblRes.value.inventory.uuid
        action.systemTags = [weight]
        action.sessionId = adminSession()
        ChangeLoadBalancerListenerAction.Result res = action.call()
        assert res.error == null
        /*tokens = LoadBalancerSystemTags.BALANCER_WEIGHT.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);

        for (Map<String, String>  token: tokens) {
            if (!vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid.equals(token.get(LoadBalancerSystemTags.BALANCER_NIC_TOKEN))) {
                continue
            }
            assert token.get(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN) == "20"
        }*/

        /*invalid weight*/
        weight = weight + "123"
        action.systemTags = [weight]
        res = action.call()
        assert res.error != null

        removeVmNicFromLoadBalancer {
            vmNicUuids = nicUuids
            listenerUuid = lblRes.value.inventory.uuid
        }

        //tokens = LoadBalancerSystemTags.BALANCER_WEIGHT.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);
        //assert tokens == null || tokens.isEmpty()

    }

    @Override
    void clean() {
        env.delete()
    }

}
