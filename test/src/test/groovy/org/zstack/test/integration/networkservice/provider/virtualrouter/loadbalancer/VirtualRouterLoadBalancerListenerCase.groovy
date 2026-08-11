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
import org.zstack.network.service.lb.LoadBalancerListenerServerGroupRefVO
import org.zstack.network.service.lb.LoadBalancerListenerServerGroupRefVO_
import org.zstack.network.service.lb.LoadBalancerListenerServerGroupVmNicRefVO
import org.zstack.network.service.lb.LoadBalancerListenerServerGroupVmNicRefVO_
import org.zstack.network.service.lb.LoadBalancerListenerVO
import org.zstack.network.service.lb.LoadBalancerListenerVO_
import org.zstack.network.service.lb.LoadBalancerSystemTags
import org.zstack.network.service.lb.LoadBalancerListenerACLRefVO_
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.stream.Collectors
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

                lb {
                    name = "lb2"
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

            vm {
                name = "vm3"
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
            testBackendServerStateAndInstancePortCase()
            testLoadBalancerWrrCase()
            testLoadBalancerAclCase()
            testOperateLBRedirectRuleCase()
            testValidateLBRedirectAclCase()
            testOperateLBRedirectAclCase()
            testDeleteAclWithMultipleListenersCase()
            testAclStatusRollbackCase()
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
        listenerAction.protocol = "http"
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

        AccessControlListInventory redirectRuleAcl = createAccessControlList {
            name = "redirect-rule"
            ipVersion = 4
        }

        //add redirect rule
        AccessControlListEntryInventory redirectRule = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/test"
            aclUuid = redirectRuleAcl.uuid
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

        LoadBalancerListenerVO listenerVO = Q.New(LoadBalancerListenerVO.class).eq(LoadBalancerListenerVO_.uuid, lblRes.value.inventory.uuid).find()

        addAccessControlListToLoadBalancer {
            aclUuids = [redirectRuleAcl.uuid]
            aclType = LoadBalancerAclType.redirect.toString()
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuids = [listenerVO.getServerGroupUuid()]
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

        removeAccessControlListFromLoadBalancer {
            aclUuids =[acl.uuid]
            listenerUuid = lblRes.value.inventory.uuid
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

        deleteAccessControlList {
            uuid = redirectRuleAcl.uuid
        }
    }

    private void testOperateLBRedirectRuleCase() {

        AccessControlListInventory acl = createAccessControlList {
            name = "redirect-acl"
        }

        AccessControlListInventory acl2 = createAccessControlList {
            name = "redirect-acl2"
        }

        AccessControlListInventory acl3 = createAccessControlList {
            name = "redirect-acl2"
        }

        /*redirect operate*/
        //add redirect rule
         AccessControlListEntryInventory redirectRule = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/test"
            aclUuid = acl.uuid
        }

        //constraint: redirect acl can only add one redirect rule
        expectError {
            addAccessControlListRedirectRule {
                name = "redirect rule"
                domain = "zstack.io"
                url = "/test"
                aclUuid = acl.uuid
            }
        }

        //constraint: domain and url cannot all null
        expectError {
            addAccessControlListRedirectRule {
                name = "redirect rule"
                domain = ""
                url = ""
                aclUuid = acl2.uuid
            }
        }

        //constraint: domain and url cannot all null
        expectError {
            addAccessControlListRedirectRule {
                name = "redirect rule"
                domain = ""
                url = "/"
                aclUuid = acl2.uuid
            }
        }

        //constraint: url must start with /
        expectError {
            addAccessControlListRedirectRule {
                name = "redirect rule"
                domain = ""
                url = "test"
                aclUuid = acl2.uuid
            }
        }
        //constraint: url must start with /
        expectError {
            addAccessControlListRedirectRule {
                name = "redirect rule"
                domain = ""
                url = "//t%s//est/"
                aclUuid = acl2.uuid
            }
        }

        addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "*test.zstack.io"
            url = ""
            aclUuid = acl2.uuid
        }

        addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/"
            aclUuid = acl3.uuid
        }

        AccessControlListEntryVO rlVO = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.uuid, redirectRule.uuid).find()
        assert rlVO != null
        assert rlVO.name == redirectRule.name
        assert rlVO.getType() == "RedirectRule" && rlVO.getType() == redirectRule.type
        assert rlVO.getMatchMethod() == "DomainAndUrl"
        assert rlVO.getCriterion() == "AccurateMatch"
        assert rlVO.getDomain() == "zstack.io"
        assert rlVO.getUrl() == "/test"

        List<AccessControlListEntryVO> rlVOs = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.aclUuid, acl2.uuid).list()
        rlVOs[0].getUrl() == "/"
        assert rlVOs[0].getType() == "RedirectRule"
        assert rlVOs[0].getMatchMethod() == "Domain"
        assert rlVOs[0].getDomain() == "*test.zstack.io"
        assert rlVOs[0].getRedirectRule() == "hdr_reg(host) -i *test.zstack.io".replace(".", "\\.").replace("*", ".*")

        rlVOs = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.aclUuid, acl3.uuid).list()
        rlVOs[0].getUrl() == "/"
        assert rlVOs[0].getType() == "RedirectRule"
        assert rlVOs[0].getMatchMethod() == "Domain"
        assert rlVOs[0].getDomain() == "zstack.io"

        //query acl
        List<AccessControlListInventory> aclList = queryAccessControlList {
            conditions = ["uuid=${acl.uuid}"]
        }
        assert aclList.get(0).entries.size() == 1
        assert aclList.get(0).entries.get(0).type == "RedirectRule"


        //constraint: cannot add redirect rule and ip entry
        expectError {
            addAccessControlListEntry {
                aclUuid = acl.uuid
                entries = "192.168.0.1,192.168.1.0/24"
            }
        }

        //constraint: redirect acl can only add one redirect rule
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
        deleteAccessControlList {
            uuid = acl2.uuid
        }
        deleteAccessControlList {
            uuid = acl3.uuid
        }
        rlVO = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.aclUuid, acl.uuid).find()
        assert rlVO == null
    }

    private void testValidateLBRedirectAclCase() {
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

        //can not attach acl to only listener
        expectError {
            addAccessControlListToLoadBalancer {
                aclUuids = [acl.uuid]
                listenerUuid = lblRes.value.inventory.uuid
                aclType = "redirect"
            }
        }

        //add acl to server group
        LoadBalancerServerGroupInventory servergroup1 = createLoadBalancerServerGroup{
            loadBalancerUuid =  load.uuid
            name = "lb-group-1"
        }
        addServerGroupToLoadBalancerListener {
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuid = servergroup1.uuid
        }

        addBackendServerToServerGroup {
            serverGroupUuid = servergroup1.uuid
            vmNics = [['uuid':vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid,'weight':'100']]
        }

        addAccessControlListToLoadBalancer {
            aclUuids = [acl.uuid]
            listenerUuid = lblRes.value.inventory.uuid
            aclType = "redirect"
            serverGroupUuids = [servergroup1.uuid]
        }
        def lbTO =cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        //assert lbTO.getRedirectRules()[0].getRedirectRule().equals("path_beg -i /test") || lbTO.getRedirectRules()[0].getRedirectRule().equals("base_reg -i zstack\\.io/test")


        //query acl redirect rule
        def lbListener = queryLoadBalancerListener {
            conditions=["uuid=$lblRes.value.inventory.uuid"]
        }[0] as LoadBalancerListenerInventory
        assert lbListener.aclRefs.size() == 1
        def acls = queryAccessControlList {
            conditions=["uuid=$acl.uuid"]
        }[0] as AccessControlListInventory
        assert acls.entries.size() == 1

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
                serverGroupUuids = [servergroup1.uuid]
            }
        }

        //constraint: cannot add acl which redirect rule listener had
        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListToLoadBalancer {
                aclUuids = [acl2.uuid]
                listenerUuid = lblRes.value.inventory.uuid
                aclType = "redirect"
                serverGroupUuids = [servergroup1.uuid]
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

        //delete load balancer listener
        deleteLoadBalancerListener {
            uuid = lblRes.value.inventory.uuid
        }

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

    private void testBackendServerStateAndInstancePortCase() {
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def vm1 = env.inventoryByName("vm") as VmInstanceInventory
        def vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        def vm3 = env.inventoryByName("vm3") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def nic1 = vm1.vmNics.find { it.l3NetworkUuid == l3.uuid }
        def nic2 = vm2.vmNics.find { it.l3NetworkUuid == l3.uuid }
        def unrelatedNic = vm3.vmNics.find { it.l3NetworkUuid == l3.uuid }

        def listener = createLoadBalancerListener {
            loadBalancerUuid = load.uuid
            name = "backend-state-listener"
            loadBalancerPort = 56
            instancePort = 56
            protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        } as LoadBalancerListenerInventory
        def serverGroup = createLoadBalancerServerGroup {
            loadBalancerUuid = load.uuid
            name = "backend-state-server-group"
        } as LoadBalancerServerGroupInventory
        addBackendServerToServerGroup {
            serverGroupUuid = serverGroup.uuid
            vmNics = [['uuid': nic1.uuid, 'weight': '20'], ['uuid': nic2.uuid, 'weight': '30']]
        }
        addServerGroupToLoadBalancerListener {
            listenerUuid = listener.uuid
            serverGroupUuid = serverGroup.uuid
        }

        List<VirtualRouterLoadBalancerBackend.RefreshLbCmd> refreshCmds =
                Collections.synchronizedList(new ArrayList<VirtualRouterLoadBalancerBackend.RefreshLbCmd>())
        boolean failNextRefresh = false
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            refreshCmds.add(JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class))
            if (failNextRefresh) {
                failNextRefresh = false
                rsp.error = "on purpose"
                rsp.success = false
            }
            return rsp
        }

        def stateResult = changeLoadBalancerListenerBackendServerState {
            listenerUuid = listener.uuid
            serverGroupUuid = serverGroup.uuid
            vmNicUuids = [nic1.uuid]
            state = "Disabled"
        }
        assert stateResult.results.size() == 1
        assert stateResult.results[0].backendType == "VmNic"
        assert stateResult.results[0].vmNicUuid == nic1.uuid
        assert stateResult.results[0].effectiveState == "Disabled"
        assert refreshCmds.size() == 1
        assert Q.New(LoadBalancerListenerServerGroupVmNicRefVO.class)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.listenerUuid, listener.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.serverGroupUuid, serverGroup.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.vmNicUuid, nic1.uuid)
                .isExists()

        def backendServers = getLoadBalancerListenerBackendServers {
            listenerUuid = listener.uuid
            serverGroupUuid = serverGroup.uuid
        }
        assert backendServers.size() == 2
        def disabledBackend = backendServers.find { it.vmNicUuid == nic1.uuid }
        assert disabledBackend.listenerUuid == listener.uuid
        assert disabledBackend.serverGroupUuid == serverGroup.uuid
        assert disabledBackend.backendType == "VmNic"
        assert disabledBackend.vmNicUuid == nic1.uuid
        assert disabledBackend.ipAddress == nic1.ip
        assert disabledBackend.weight == 20
        assert disabledBackend.state == "Disabled"
        assert disabledBackend.runtimeStatus == "Active"
        assert disabledBackend.healthStatus == "Unchecked"
        assert disabledBackend.instancePort == 56
        def enabledBackend = backendServers.find { it.vmNicUuid == nic2.uuid }
        assert enabledBackend.listenerUuid == listener.uuid
        assert enabledBackend.serverGroupUuid == serverGroup.uuid
        assert enabledBackend.backendType == "VmNic"
        assert enabledBackend.vmNicUuid == nic2.uuid
        assert enabledBackend.ipAddress == nic2.ip
        assert enabledBackend.weight == 30
        assert enabledBackend.state == "Enabled"
        assert enabledBackend.runtimeStatus == "Active"
        assert enabledBackend.healthStatus == "Unknown"
        assert enabledBackend.instancePort == 56

        changeLoadBalancerListenerBackendServerState {
            listenerUuid = listener.uuid
            serverGroupUuid = serverGroup.uuid
            vmNicUuids = [nic1.uuid]
            state = "Disabled"
        }
        assert refreshCmds.size() == 1

        ChangeLoadBalancerListenerBackendServerStateAction invalidStateAction =
                new ChangeLoadBalancerListenerBackendServerStateAction()
        invalidStateAction.listenerUuid = listener.uuid
        invalidStateAction.serverGroupUuid = serverGroup.uuid
        invalidStateAction.vmNicUuids = [nic2.uuid, unrelatedNic.uuid]
        invalidStateAction.state = "Disabled"
        invalidStateAction.sessionId = adminSession()
        assert invalidStateAction.call().error != null
        assert refreshCmds.size() == 1
        assert !Q.New(LoadBalancerListenerServerGroupVmNicRefVO.class)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.listenerUuid, listener.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.serverGroupUuid, serverGroup.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.vmNicUuid, nic2.uuid)
                .isExists()
        backendServers = getLoadBalancerListenerBackendServers {
            listenerUuid = listener.uuid
            serverGroupUuid = serverGroup.uuid
        }
        assert backendServers.find { it.vmNicUuid == nic2.uuid }.state == "Enabled"
        assert backendServers.find { it.vmNicUuid == nic2.uuid }.healthStatus == "Unknown"

        failNextRefresh = true
        ChangeLoadBalancerListenerBackendServerStateAction rollbackStateAction =
                new ChangeLoadBalancerListenerBackendServerStateAction()
        rollbackStateAction.listenerUuid = listener.uuid
        rollbackStateAction.serverGroupUuid = serverGroup.uuid
        rollbackStateAction.vmNicUuids = [nic2.uuid]
        rollbackStateAction.state = "Disabled"
        rollbackStateAction.sessionId = adminSession()
        assert rollbackStateAction.call().error != null
        retryInSecs {
            assert refreshCmds.size() == 3
        }
        assert !Q.New(LoadBalancerListenerServerGroupVmNicRefVO.class)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.listenerUuid, listener.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.serverGroupUuid, serverGroup.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.vmNicUuid, nic2.uuid)
                .isExists()
        backendServers = getLoadBalancerListenerBackendServers {
            listenerUuid = listener.uuid
            serverGroupUuid = serverGroup.uuid
        }
        assert backendServers.find { it.vmNicUuid == nic2.uuid }.state == "Enabled"
        assert backendServers.find { it.vmNicUuid == nic2.uuid }.healthStatus == "Unknown"

        changeLoadBalancerListenerBackendServerState {
            listenerUuid = listener.uuid
            serverGroupUuid = serverGroup.uuid
            vmNicUuids = [nic1.uuid]
            state = "Enabled"
        }
        assert refreshCmds.size() == 4
        assert !Q.New(LoadBalancerListenerServerGroupVmNicRefVO.class)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.listenerUuid, listener.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.serverGroupUuid, serverGroup.uuid)
                .eq(LoadBalancerListenerServerGroupVmNicRefVO_.vmNicUuid, nic1.uuid)
                .isExists()

        backendServers = getLoadBalancerListenerBackendServers {
            listenerUuid = listener.uuid
            serverGroupUuid = serverGroup.uuid
        }
        assert backendServers.find { it.vmNicUuid == nic1.uuid }.state == "Enabled"
        assert backendServers.find { it.vmNicUuid == nic1.uuid }.healthStatus == "Unknown"

        int beforePortRollback = refreshCmds.size()
        failNextRefresh = true
        ChangeLoadBalancerListenerAction portAction = new ChangeLoadBalancerListenerAction()
        portAction.uuid = listener.uuid
        portAction.instancePort = 5656
        portAction.sessionId = adminSession()
        assert portAction.call().error != null
        retryInSecs {
            assert refreshCmds.size() == beforePortRollback + 2
        }
        assert Q.New(LoadBalancerListenerVO.class)
                .select(LoadBalancerListenerVO_.instancePort)
                .eq(LoadBalancerListenerVO_.uuid, listener.uuid)
                .findValue() == 56
        assert refreshCmds[beforePortRollback].lbs.find { it.listenerUuid == listener.uuid }.instancePort == 5656
        assert refreshCmds[beforePortRollback + 1].lbs.find { it.listenerUuid == listener.uuid }.instancePort == 56

        int beforePortSuccess = refreshCmds.size()
        portAction = new ChangeLoadBalancerListenerAction()
        portAction.uuid = listener.uuid
        portAction.instancePort = 5656
        portAction.sessionId = adminSession()
        assert portAction.call().error == null
        assert refreshCmds.size() == beforePortSuccess + 1
        assert Q.New(LoadBalancerListenerVO.class)
                .select(LoadBalancerListenerVO_.instancePort)
                .eq(LoadBalancerListenerVO_.uuid, listener.uuid)
                .findValue() == 5656
        backendServers = getLoadBalancerListenerBackendServers {
            listenerUuid = listener.uuid
            serverGroupUuid = serverGroup.uuid
        }
        assert backendServers.every { it.instancePort == 5656 }

        portAction = new ChangeLoadBalancerListenerAction()
        portAction.uuid = listener.uuid
        portAction.instancePort = 5656
        portAction.sessionId = adminSession()
        assert portAction.call().error == null
        assert refreshCmds.size() == beforePortSuccess + 1

        deleteLoadBalancerListener {
            uuid = listener.uuid
        }
        deleteLoadBalancerServerGroup {
            uuid = serverGroup.uuid
        }
    }

    private void testOperateLBRedirectAclCase() {
        /*init env,create load balancer listener,create server group, add vm nics to listener*/
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        def vm3 = env.inventoryByName("vm3") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def _name = "test7"

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        CreateLoadBalancerListenerAction listenerAction = new CreateLoadBalancerListenerAction()
        listenerAction.loadBalancerUuid = load.uuid
        listenerAction.name = _name
        listenerAction.loadBalancerPort = 8903
        listenerAction.instancePort = 8903
        listenerAction.protocol = "https"
        listenerAction.sessionId = adminSession()

        CreateLoadBalancerListenerAction.Result lblRes = listenerAction.call()
        assert lblRes.error == null

        LoadBalancerServerGroupInventory servergroup7 = createLoadBalancerServerGroup{
            loadBalancerUuid =  load.uuid
            name = "lb-sg-7"
        }

        //add default server group
        addVmNicToLoadBalancer {
            vmNicUuids = [vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid]
            listenerUuid = lblRes.value.inventory.uuid
        }
        LoadBalancerListenerServerGroupRefVO lbSgRef = Q.New(LoadBalancerListenerServerGroupRefVO.class).eq(LoadBalancerListenerServerGroupRefVO_.listenerUuid, lblRes.value.inventory.uuid).find()
        LoadBalancerListenerVO listenerVO = Q.New(LoadBalancerListenerVO.class).eq(LoadBalancerListenerVO_.uuid, lblRes.value.inventory.uuid).find()
        assert listenerVO.getServerGroupUuid().equals(lbSgRef.getServerGroupUuid())

        /*create acl*/
        AccessControlListInventory acl = createAccessControlList {
            name = "redirect-acl-test-7"
        }
        AccessControlListInventory acl2 = createAccessControlList {
            name = "redirect-acl-test-7-2"
        }

        /*create acl redirect rule*/
        AccessControlListEntryInventory redirectRule1 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/test"
            aclUuid = acl.uuid
        }
        changeAccessControlListRedirectRule {
            uuid = redirectRule1.uuid
            name = "redirect rule test"
        }
        def rName = Q.New(AccessControlListEntryVO.class).eq(AccessControlListEntryVO_.uuid, redirectRule1.uuid).select(AccessControlListEntryVO_.name).findValue()
        assert rName == "redirect rule test"

        /*add ip entry to acl which has redirect rule*/
        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListEntry {
                aclUuid = acl.uuid
                entries = "192.168.0.1,192.168.1.0/24"
            }
        }

        /*add acl redirect rule to acl which has one redirect rule*/
        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListRedirectRule {
                name = "redirect rule"
                domain = "zstack.io"
                url = "/test/test"
                aclUuid = acl.uuid
            }
        }

        /*query listener acl entry*/
        def inventoryMap = getLoadBalancerListenerACLEntries {
            listenerUuids = [lblRes.value.inventory.uuid]
        }
        assert inventoryMap.isEmpty()

        /*constraint: cannot attach acl to only listener*/
        cmd = null
        expect( [ApiException.class, AssertionError.class] ) {
            addAccessControlListToLoadBalancer {
                aclUuids = [acl.uuid]
                aclType = "redirect"
                listenerUuid = lblRes.value.inventory.uuid
                serverGroupUuids = [servergroup7.uuid]
            }
        }
        assert cmd == null
        List<LoadBalancerListenerACLRefVO> listenerACLRefVOs = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.listenerUuid, lblRes.value.inventory.uuid).list()
        assert listenerACLRefVOs.size() == 0

        /*query listener acl entry*/
        inventoryMap = getLoadBalancerListenerACLEntries {
            listenerUuids = [lblRes.value.inventory.uuid]
        }
        assert inventoryMap.size() == 0

        //attach server group to listener
        addServerGroupToLoadBalancerListener {
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuid = servergroup7.uuid
        }

        cmd = null
        /*attach acl to server group, not refresh because no vm nic*/
        addAccessControlListToLoadBalancer {
            aclUuids = [acl.uuid]
            aclType = "redirect"
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuids = [servergroup7.uuid]
        }
        listenerACLRefVOs = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.listenerUuid, lblRes.value.inventory.uuid).list()
        assert listenerACLRefVOs.size() == 1
        assert listenerACLRefVOs[0].getServerGroupUuid() == servergroup7.uuid
        assert cmd == null

        /*add vm nics to server group*/
        addBackendServerToServerGroup {
            serverGroupUuid = servergroup7.uuid
            vmNics = [['uuid':vm2.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid,'weight':'100']]
        }
        def lbTO =cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        def dfSg = lbTO.serverGroups.stream().filter{it.getServerGroupUuid().equals("defaultServerGroup")}.collect(Collectors.toList())[0]
        assert dfSg.name.equals("default-server-group")
        assert lbTO.getServerGroups().size() == 2
        assert lbTO.getRedirectRules().size() == 1
        def redirectRule= lbTO.redirectRules.stream().filter{ it -> it.getServerGroupUuid().equals(servergroup7.uuid)}.collect(Collectors.toList())[0]
        assert redirectRule.aclUuid == acl.uuid
        //assert redirectRule.redirectRule.matches('base_reg -i zstack\\.io/test')
        assert redirectRule.redirectRuleUuid.equals(redirectRule1.uuid)

        /*create other redirect rule, add to acl2*/
        AccessControlListEntryInventory redirectRule2 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "*.zstack.io"
            url = "/test"
            aclUuid = acl2.uuid
        }

        /*attach acl2 to sg7*/
        addAccessControlListToLoadBalancer {
            aclUuids = [acl2.uuid]
            aclType = "redirect"
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuids = [servergroup7.uuid]
        }

        LoadBalancerServerGroupInventory servergroup8 = createLoadBalancerServerGroup{
            loadBalancerUuid =  load.uuid
            name = "lb-sg-8"
        }
        addServerGroupToLoadBalancerListener {
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuid = servergroup8.uuid
        }
        addBackendServerToServerGroup {
            serverGroupUuid = servergroup8.uuid
            vmNics = [['uuid':vm3.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid,'weight':'100']]
        }
        /*attach acl to server group, not refresh because no vm nic*/
        cmd = null
        LoadBalancerListerAcl res = changeAccessControlListServerGroup {
            listenerUuid = lblRes.value.inventory.uuid
            aclUuid = acl2.uuid
            serverGroupUuids = [servergroup8.uuid]
        } as LoadBalancerListerAcl
        listenerACLRefVOs = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.aclUuid, acl.uuid).eq(LoadBalancerListenerACLRefVO_.listenerUuid, lblRes.value.inventory.uuid).list()
        assert listenerACLRefVOs.size() == 1
        assert listenerACLRefVOs[0].serverGroupUuid == servergroup7.uuid

        removeAccessControlListFromLoadBalancer {
            serverGroupUuids = [servergroup8.uuid]
            listenerUuid = lblRes.value.inventory.uuid
            aclUuids = [acl2.uuid]
        }


        cmd = null
        addAccessControlListToLoadBalancer {
            aclUuids = [acl2.uuid]
            aclType = "redirect"
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuids = [servergroup8.uuid]
        }
        sleep(5000)
        lbTO =cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        assert lbTO.getServerGroups().size() == 3
        assert lbTO.getRedirectRules().size() == 2

        dfSg = lbTO.serverGroups.stream().filter{it.getServerGroupUuid().equals("defaultServerGroup")}.collect(Collectors.toList())[0]
        assert dfSg.name.equals("default-server-group")
        assert dfSg.getBackendServers().size() == 1
        assert dfSg.getBackendServers()[0].getIp() == vm.getVmNics()[0].ip

        dfSg = lbTO.serverGroups.stream().filter{it.getServerGroupUuid().equals(servergroup7.uuid)}.collect(Collectors.toList())[0]
        assert dfSg.getBackendServers().size() == 1
        assert dfSg.getBackendServers()[0].getIp() == vm2.getVmNics()[0].ip

        dfSg = lbTO.serverGroups.stream().filter{it.getServerGroupUuid().equals(servergroup8.uuid)}.collect(Collectors.toList())[0]
        assert dfSg.getBackendServers().size() == 1
        assert dfSg.getBackendServers()[0].getIp() == vm3.getVmNics()[0].ip

        redirectRule = lbTO.redirectRules.stream().filter{ it -> it.getServerGroupUuid().equals(servergroup7.uuid)}.collect(Collectors.toList())[0]
        assert redirectRule.aclUuid == acl.uuid
        //assert redirectRule.redirectRule.matches('base_reg -i zstack\\.io/test')
        assert redirectRule.redirectRuleUuid.equals(redirectRule1.uuid)

        redirectRule = lbTO.redirectRules.stream().filter{ it -> it.getServerGroupUuid().equals(servergroup8.uuid)}.collect(Collectors.toList())[0]
        assert redirectRule.aclUuid == acl2.uuid
        assert redirectRule.redirectRuleUuid.equals(redirectRule2.uuid)

        /*change acl server group*/
        cmd = null
        res = changeAccessControlListServerGroup {
            listenerUuid = lblRes.value.inventory.uuid
            aclUuid = acl2.uuid
            serverGroupUuids = [servergroup7.uuid, servergroup8.uuid]
        } as LoadBalancerListerAcl

        assert res.getServerGroupUuids().size() == 2
        assert res.getServerGroupUuids().contains(servergroup7.uuid)
        assert res.getServerGroupUuids().contains(servergroup8.uuid)

        List<LoadBalancerListenerACLRefVO> lblAclRefVOs = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.listenerUuid, lblRes.value.inventory.uuid).eq(LoadBalancerListenerACLRefVO_.aclUuid, acl2.uuid).list()
        assert lblAclRefVOs.size() == 2
        assert [servergroup7.uuid, servergroup8.uuid].contains(lblAclRefVOs[0].getServerGroupUuid()) &&  [servergroup7.uuid, servergroup8.uuid].contains(lblAclRefVOs[1].getServerGroupUuid())

        sleep(3000)
        lbTO =cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        assert lbTO.getServerGroups().size() == 3
        assert lbTO.getRedirectRules().size() == 2
        List<VirtualRouterLoadBalancerBackend.LbTO.ServerGroup> serverGroupList1 = lbTO.getServerGroups()
        List<String> ips1 = lbTO.getNicIps()


        cmd = null
        def vrUuids = Q.New(VirtualRouterVmVO.class).select(VirtualRouterVmVO_.uuid).listValues()
        reconnectVirtualRouter {
            vmInstanceUuid = vrUuids[0]
        }
        sleep(1000)
        lbTO =cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        List<VirtualRouterLoadBalancerBackend.LbTO.ServerGroup> serverGroupList2 = lbTO.getServerGroups()
        List<String> ips2 = lbTO.getNicIps()

        assert serverGroupList1.size() == serverGroupList2.size()
        assert ips1.size() == ips2.size()
        for (int i = 0; i < serverGroupList1.size(); i++) {
            assert serverGroupList1[i].serverGroupUuid == serverGroupList2[i].serverGroupUuid
            for(int j = 0; j <serverGroupList1[i].getBackendServers().size(); j ++) {
                assert serverGroupList1[i].getBackendServers().get(j).getIp() == serverGroupList2[i].getBackendServers().get(j).getIp()
            }
        }

        for (int i = 0; i < ips1.size(); i++) {
            assert ips1[i] == ips2[i]
        }

        /*query listener acl entry*/
        inventoryMap = getLoadBalancerListenerACLEntries {
            listenerUuids = [lblRes.value.inventory.uuid]
        }
        assert inventoryMap.size() == 1

        cmd = null
        /*attach acl to default server group*/
        addAccessControlListToLoadBalancer {
            aclUuids = [acl.uuid]
            aclType = "redirect"
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuids = [listenerVO.serverGroupUuid]
        }
        sleep(1000)
        lbTO =cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        assert lbTO.getServerGroups().size() == 2
        def sg =  lbTO.getServerGroups().stream().filter{sg -> sg.getBackendServers().size() == 2}.collect(Collectors.toList())
        assert sg.size() == 2

        cmd = null
        /*remove vm from sg7*/
        removeBackendServerFromServerGroup {
            serverGroupUuid = servergroup7.uuid
            vmNicUuids = [vm2.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid]
        }
        sleep(1000)
        lbTO =cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        assert lbTO.getServerGroups().size() == 2
        sg =  lbTO.getServerGroups().stream().filter{ it.getBackendServers().size() == 2}.collect(Collectors.toList())
        assert !lbTO.getServerGroups().stream().anyMatch({it.serverGroupUuid == servergroup7.uuid})
        assert lbTO.getServerGroups().stream().anyMatch({it.serverGroupUuid == servergroup8.uuid})
        assert sg.size() == 0

        removeServerGroupFromLoadBalancerListener {
            serverGroupUuid = listenerVO.serverGroupUuid
            listenerUuid = listenerVO.uuid
        }
        sleep(1000)
        lbTO =cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        assert lbTO.getServerGroups().size() == 1
        assert lbTO.getServerGroups()[0].getBackendServers().size() == 1
        assert lbTO.getServerGroups()[0].getServerGroupUuid() == servergroup8.uuid
        assert lbTO.getRedirectRules().size() == 1

        cmd = null
        /*remove acl from listener*/
        /*remove acl from listener sg7*/
        removeAccessControlListFromLoadBalancer {
            aclUuids = [acl.uuid]
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuids = [servergroup7.uuid]
        }
        sleep(1000)
        assert cmd == null

        cmd = null
        /*remove acl from sg8 when sg has backend server*/
        removeAccessControlListFromLoadBalancer {
            aclUuids = [acl2.uuid]
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuids = [servergroup8.uuid]
        }
        sleep(1000)
        lbTO =cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        assert lbTO.getServerGroups().size() == 0
        assert lbTO.getRedirectRules().size() == 0

        cmd = null
        /*delete acl when acl has serverGroup and sg has vm nic*/
        deleteAccessControlList {
            uuid = acl2.uuid
        }
        sleep(1000)
        assert cmd == null

        /*test sort*/
        AccessControlListInventory acl3 = createAccessControlList {
            name = "redirect-acl-test-7-3"
        }
        AccessControlListInventory acl4 = createAccessControlList {
            name = "redirect-acl-test-7-4"
        }
        AccessControlListInventory acl5 = createAccessControlList {
            name = "redirect-acl-test-7-5"
        }
        AccessControlListInventory acl6 = createAccessControlList {
            name = "redirect-acl-test-7-6"
        }
        AccessControlListInventory acl7 = createAccessControlList {
            name = "redirect-acl-test-7-7"
        }
        AccessControlListInventory acl8 = createAccessControlList {
            name = "redirect-acl-test-7-8"
        }

        acl8 = updateAccessControlList {
            uuid = acl8.uuid
            name = "redirect-acl-test-7-8-update"
            description = "redirect-acl-test-7-8-update"
        }
        AccessControlListEntryInventory redirectRule3 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "*.zstack.io"
            url = "/index.html"
            aclUuid = acl3.uuid
        }
        AccessControlListEntryInventory redirectRule4 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "*.zstack.io"
            url = ""
            aclUuid = acl4.uuid
        }
        AccessControlListEntryInventory redirectRule5 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/test"
            aclUuid = acl5.uuid
        }
        AccessControlListEntryInventory redirectRule6 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "zstack.io"
            url = "/"
            aclUuid = acl6.uuid
        }
        AccessControlListEntryInventory redirectRule7 = addAccessControlListRedirectRule {
            name = "redirect rule"
            domain = "*est.zstack.io"
            url = "/"
            aclUuid = acl8.uuid
        }

        cmd = null
        addAccessControlListToLoadBalancer {
            aclUuids = [acl3.uuid, acl4.uuid, acl5.uuid, acl6.uuid, acl7.uuid, acl8.uuid]
            listenerUuid = lblRes.value.inventory.uuid
            serverGroupUuids = [servergroup8.uuid]
            aclType = "redirect"
        }
        sleep(1000)
        lbTO = cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        List<VirtualRouterLoadBalancerBackend.LbTO.RedirectRule> redirectRules = lbTO.getRedirectRules()
        assert redirectRules.get(0).getRedirectRule().equals(("base_reg -i zstack.io:8903/test").replace(".", "\\."))
        assert redirectRules.get(1).getRedirectRule().equals(("hdr_reg(host) -i zstack.io").replace(".", "\\."))
        assert redirectRules.get(2).getRedirectRule().equals(("hdr_reg(host) -i *est.zstack.io").replace(".", "\\.").replace("*", ".*"))
        assert redirectRules.get(3).getRedirectRule().equals(("base_reg -i *.zstack.io:8903").replace(".", "\\.").replace("*", ".*")+"/index.html")
        assert redirectRules.get(4).getRedirectRule().equals(("hdr_reg(host) -i *.zstack.io").replace(".", "\\.").replace("*", ".*"))

        cmd = null
        /*delete acl when acl has serverGroup and sg has vm nic*/
        deleteAccessControlList {
            uuid = acl3.uuid
        }
        sleep(1000)
        lbTO = cmd.lbs.stream().filter{lb -> lb.listenerUuid.equals(lblRes.value.inventory.uuid)}.collect(Collectors.toList())[0] as VirtualRouterLoadBalancerBackend.LbTO
        assert lbTO.getRedirectRules().size() == 4

        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            rsp.error = "failed"
            rsp.success = false
            return rsp
        }
        expectError {
            /*delete acl when acl has serverGroup and sg has vm nic*/
            deleteAccessControlList {
                uuid = acl4.uuid
            }
        }
        List<LoadBalancerListenerACLRefVO> aclRefVOList = Q.New(LoadBalancerListenerACLRefVO.class).eq(LoadBalancerListenerACLRefVO_.listenerUuid, lblRes.value.inventory.uuid).list()
        aclRefVOList.size() == 4

        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        //delete load balancer listener
        deleteLoadBalancerListener {
            uuid = lblRes.value.inventory.uuid
        }

        //delete acl
        deleteAccessControlList {
            uuid = acl.uuid
        }
        deleteAccessControlList {
            uuid = acl2.uuid
        }

        //delete acl
        deleteAccessControlList {
            uuid = acl4.uuid
        }
        deleteAccessControlList {
            uuid = acl5.uuid
        }

        //delete acl
        deleteAccessControlList {
            uuid = acl6.uuid
        }
        deleteAccessControlList {
            uuid = acl7.uuid
        }

        deleteAccessControlList {
            uuid = acl8.uuid
        }
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

    private void testDeleteAclWithMultipleListenersCase() {
        def load = env.inventoryByName("lb2") as LoadBalancerInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        int refreshCount = 0
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            refreshCount++
            return rsp
        }

        // Create multiple listeners associated with the same load balancer
        List<LoadBalancerListenerInventory> listeners = new ArrayList<>()
        int listenerCount = 3 // Create 3 listeners to ensure multiple associations
        for (int i = 0; i < listenerCount; i++) {
            CreateLoadBalancerListenerAction listenerAction = new CreateLoadBalancerListenerAction()
            listenerAction.loadBalancerUuid = load.uuid
            listenerAction.name = "listener-${i}"
            listenerAction.loadBalancerPort = 9000 + i
            listenerAction.instancePort = 9000 + i
            listenerAction.protocol = "udp"
            listenerAction.sessionId = adminSession()
            CreateLoadBalancerListenerAction.Result lblRes = listenerAction.call()
            assert lblRes.error == null
            listeners.add(lblRes.value.inventory)

            // Add VM NIC to each listener to ensure backend server group exists
            addVmNicToLoadBalancer {
                vmNicUuids = [vm.vmNics.find { nic -> nic.l3NetworkUuid == l3.uuid }.uuid]
                listenerUuid = lblRes.value.inventory.uuid
            }
        }

        // Create an ACL
        AccessControlListInventory acl = createAccessControlList {
            name = "acl-multi-listeners"
        }
        addAccessControlListEntry {
            aclUuid = acl.uuid
            entries = "192.168.0.1,192.168.1.0/24"
        }

        // Associate the ACL with all listeners
        listeners.each { listener ->
            addAccessControlListToLoadBalancer {
                aclUuids = [acl.uuid]
                aclType = LoadBalancerAclType.black.toString()
                listenerUuid = listener.uuid
            }
        }

        // Verify that ACL is associated with all listeners
        listeners.each { listener ->
            def refs = Q.New(LoadBalancerListenerACLRefVO.class)
                    .eq(LoadBalancerListenerACLRefVO_.listenerUuid, listener.uuid)
                    .eq(LoadBalancerListenerACLRefVO_.aclUuid, acl.uuid)
                    .list()
            assert refs.size() == 1
        }

        // Delete the ACL and verify all associations are removed
        cmd = null
        refreshCount = 0
        deleteAccessControlList {
            uuid = acl.uuid
        }

        // Retry to ensure the deletion operation is completed and messages are sent
        retryInSecs {
            assert cmd != null
            assert cmd.lbs.size() == listenerCount
            assert refreshCount == listenerCount
        }

        // Verify that all LoadBalancerListenerACLRefVO records are removed
        listeners.each { listener ->
            def refs = Q.New(LoadBalancerListenerACLRefVO.class)
                    .eq(LoadBalancerListenerACLRefVO_.listenerUuid, listener.uuid)
                    .eq(LoadBalancerListenerACLRefVO_.aclUuid, acl.uuid)
                    .list()
            assert refs.isEmpty()
        }

        // Verify that AccessControlListEntryVO records are also removed
        def entries = Q.New(AccessControlListEntryVO.class)
                .eq(AccessControlListEntryVO_.aclUuid, acl.uuid)
                .list()
        assert entries.isEmpty()
    }

    void testAclStatusRollbackCase() {
        def lb = env.inventoryByName("lb") as LoadBalancerInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory
        def _name = "testacl"

        CreateLoadBalancerListenerAction listenerAction = new CreateLoadBalancerListenerAction()
        listenerAction.loadBalancerUuid = lb.uuid
        listenerAction.name = _name
        listenerAction.loadBalancerPort = 8080
        listenerAction.instancePort = 8080
        listenerAction.protocol = "http"
        listenerAction.sessionId = adminSession()
        CreateLoadBalancerListenerAction.Result createRes = listenerAction.call()
        assert createRes.error == null
        def listenerUuid_lb = createRes.value.inventory.uuid

        List<String> nicUuids = [vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid, vm2.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid]
        addVmNicToLoadBalancer {
            vmNicUuids = nicUuids
            listenerUuid = listenerUuid_lb
        }

        def oldAclStatus = LoadBalancerSystemTags.BALANCER_ACL.getTokenByResourceUuid(listenerUuid_lb, LoadBalancerSystemTags.BALANCER_ACL_TOKEN)
        assert oldAclStatus == "disable"

        env.simulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            VirtualRouterLoadBalancerBackend.RefreshLbRsp rspFail = new VirtualRouterLoadBalancerBackend.RefreshLbRsp()
            rspFail.setError("Acl refresh failed")
            rspFail.setSuccess(false)
            return rspFail
        }

        ChangeLoadBalancerListenerAction changeAction = new ChangeLoadBalancerListenerAction()
        changeAction.uuid = listenerUuid_lb
        changeAction.aclStatus = LoadBalancerAclStatus.enable.toString()
        changeAction.sessionId = adminSession()
        ChangeLoadBalancerListenerAction.Result changeRes = changeAction.call()
        def rollbackedStatus = LoadBalancerSystemTags.BALANCER_ACL.getTokenByResourceUuid(listenerUuid_lb, LoadBalancerSystemTags.BALANCER_ACL_TOKEN)
        assert rollbackedStatus == "disable"
    }

    @Override
    void clean() {
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        deleteLoadBalancer {
            uuid = load.uuid
        }
        assert Q.New(LoadBalancerListenerACLRefVO.class).count() == 0
        env.delete()
    }

}
