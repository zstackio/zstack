package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.appliancevm.ApplianceVmVO_
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerListenerVO
import org.zstack.network.service.lb.LoadBalancerListenerVO_
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.network.service.lb.LoadBalancerVO_
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.ChangeLoadBalancerListenerAction
import org.zstack.sdk.ApiResult
import org.zstack.sdk.CreateLoadBalancerListenerAction
import org.zstack.sdk.CreateLoadBalancerListenerResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.LoadBalancerServerGroupInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZSClient
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.springframework.http.HttpEntity

import java.util.concurrent.CopyOnWriteArrayList

class TcpIpvsLoadBalancerListenerApiCase extends SubCase {
    EnvSpec env
    LoadBalancerInventory lb
    List<VirtualRouterLoadBalancerBackend.RefreshLbCmd> refreshCmds = new CopyOnWriteArrayList<>()

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
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
                }
            }
            zone {
                name = "zone"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                        totalCpu = 8
                        totalMem = SizeUnit.GIGABYTE.toByte(12)
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
                        name = "publicL3"
                        ip {
                            startIp = "172.20.58.160"
                            endIp = "172.20.58.200"
                            gateway = "172.20.0.1"
                            netmask = "255.255.0.0"
                        }
                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }
                    }

                    l3Network {
                        name = "backendL3"
                        ip {
                            startIp = "10.58.0.160"
                            endIp = "10.58.0.200"
                            gateway = "10.58.0.1"
                            netmask = "255.255.255.0"
                        }
                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING]
                        }
                    }

                    l3Network {
                        name = "managementL3"
                        ip {
                            startIp = "172.21.58.160"
                            endIp = "172.21.58.200"
                            gateway = "172.21.0.1"
                            netmask = "255.255.0.0"
                        }
                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString()]
                        }
                    }
                }
                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("managementL3")
                    usePublicL3Network("publicL3")
                    useImage("vr")
                    isDefault = true
                }
            }

            vm {
                name = "backend-vm-1"
                useImage("image")
                useL3Networks("backendL3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "backend-vm-2"
                useImage("image")
                useL3Networks("backendL3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "backend-vm-3"
                useImage("image")
                useL3Networks("backendL3")
                useInstanceOffering("instanceOffering")
            }

            vm {
                name = "backend-vm-4"
                useImage("image")
                useL3Networks("backendL3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepareDedicatedLoadBalancer()
            testTcpHaproxyDefaultDataPlane()
            testTcpIpvsFullNatListener()
            testTcpIpvsDefaultForwardMode()
            testTcpIpvsCreateValidation()
            testTcpIpvsDedicatedListenerBackendRefreshPayload()
        }
    }

    void prepareDedicatedLoadBalancer() {
        L3NetworkInventory publicL3 = env.inventoryByName("publicL3") as L3NetworkInventory
        VipInventory vip = createVip {
            delegate.name = "tcp-ipvs-api-vip"
            delegate.l3NetworkUuid = publicL3.uuid
        }

        lb = createLoadBalancer {
            delegate.name = "tcp-ipvs-api-lb"
            delegate.vipUuid = vip.uuid
            delegate.systemTags = ["separateVirtualRouterVm"]
        }
    }

    void testTcpHaproxyDefaultDataPlane() {
        CreateLoadBalancerListenerAction action = new CreateLoadBalancerListenerAction()
        action.name = "tcp-haproxy-default"
        action.loadBalancerUuid = lb.uuid
        action.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        action.loadBalancerPort = 11080
        action.instancePort = 8080
        action.sessionId = adminSession()

        ApiResult result = ZSClient.call(action)
        assert result.error == null

        String rawResult = getApiResultString(result)
        assert !rawResult.contains("\"dataPlane\"")
        assert !rawResult.contains("\"forwardMode\"")

        LoadBalancerListenerInventory listener = result.getResult(CreateLoadBalancerListenerResult.class).inventory

        assert listener.dataPlane == null
        assert listener.forwardMode == null

        LoadBalancerListenerVO vo = Q.New(LoadBalancerListenerVO.class)
                .eq(LoadBalancerListenerVO_.uuid, listener.uuid)
                .find()
        assert vo.dataPlane == LoadBalancerConstants.DATA_PLANE_HAPROXY
        assert vo.forwardMode == null
    }

    void testTcpIpvsFullNatListener() {
        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            delegate.name = "tcp-ipvs-full-nat"
            delegate.loadBalancerUuid = lb.uuid
            delegate.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
            delegate.loadBalancerPort = 11081
            delegate.instancePort = 8080
            delegate.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
            delegate.forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        }

        assert listener.dataPlane == LoadBalancerConstants.DATA_PLANE_IPVS
        assert listener.forwardMode == LoadBalancerConstants.FORWARD_MODE_FULL_NAT

        LoadBalancerListenerVO vo = Q.New(LoadBalancerListenerVO.class)
                .eq(LoadBalancerListenerVO_.uuid, listener.uuid)
                .find()
        assert vo.dataPlane == LoadBalancerConstants.DATA_PLANE_IPVS
        assert vo.forwardMode == LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        assert vo.instancePort == 8080
    }

    void testTcpIpvsDefaultForwardMode() {
        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            delegate.name = "tcp-ipvs-default-forward-mode"
            delegate.loadBalancerUuid = lb.uuid
            delegate.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
            delegate.loadBalancerPort = 11082
            delegate.instancePort = 8080
            delegate.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
        }

        assert listener.dataPlane == LoadBalancerConstants.DATA_PLANE_IPVS
        assert listener.forwardMode == LoadBalancerConstants.FORWARD_MODE_FULL_NAT
    }

    void testTcpIpvsCreateValidation() {
        assertCreateListenerError(11083, LoadBalancerConstants.LB_PROTOCOL_HTTP,
                LoadBalancerConstants.DATA_PLANE_IPVS, LoadBalancerConstants.FORWARD_MODE_FULL_NAT)
        assertCreateListenerError(11084, LoadBalancerConstants.LB_PROTOCOL_UDP,
                LoadBalancerConstants.DATA_PLANE_IPVS, LoadBalancerConstants.FORWARD_MODE_FULL_NAT)
        assertCreateListenerError(11085, LoadBalancerConstants.LB_PROTOCOL_TCP,
                LoadBalancerConstants.DATA_PLANE_HAPROXY, LoadBalancerConstants.FORWARD_MODE_FULL_NAT)
        CreateLoadBalancerListenerAction.Result natResult = assertCreateListenerError(11086, LoadBalancerConstants.LB_PROTOCOL_TCP,
                LoadBalancerConstants.DATA_PLANE_IPVS, LoadBalancerConstants.FORWARD_MODE_NAT)
        assert natResult.error.details.contains("TCP IPVS only supports forwardMode[full_nat]")

        CreateLoadBalancerListenerAction.Result drResult = assertCreateListenerError(11087, LoadBalancerConstants.LB_PROTOCOL_TCP,
                LoadBalancerConstants.DATA_PLANE_IPVS, LoadBalancerConstants.FORWARD_MODE_DR)
        assert drResult.error.details.contains("TCP IPVS only supports forwardMode[full_nat]")
    }

    void testTcpIpvsDedicatedListenerBackendRefreshPayload() {
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            refreshCmds.add(JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class))
            return rsp
        }
        List<KVMAgentCommands.DestroyVmCmd> destroyVmCmds = new CopyOnWriteArrayList<>()
        env.afterSimulator(KVMConstant.KVM_DESTROY_VM_PATH) { rsp, HttpEntity<String> e ->
            destroyVmCmds.add(JSONObjectUtil.toObject(e.body, KVMAgentCommands.DestroyVmCmd.class))
            return rsp
        }

        LoadBalancerListenerInventory listener = createTcpIpvsListener("tcp-ipvs-t2", 11090, LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN)

        String backendIp1 = backendIp("backend-vm-1")
        String backendIp2 = backendIp("backend-vm-2")
        String backendIp3 = backendIp("backend-vm-3")
        String backendIp4 = backendIp("backend-vm-4")

        LoadBalancerServerGroupInventory group1 = createServerGroupWithVmNics(
                "tcp-ipvs-t2-group-1",
                [backendVmNic("backend-vm-1", "100")])
        addServerGroupToLoadBalancerListener {
            listenerUuid = listener.uuid
            serverGroupUuid = group1.uuid
        }

        VirtualRouterLoadBalancerBackend.LbTO to = lastLbTO(listener.uuid)
        assertTcpIpvsTO(to, listener.uuid, 11090, LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN)
        assertServerGroups(to, [
                (group1.uuid): [(backendIp1): 100L]
        ])

        addBackendServerToServerGroup {
            serverGroupUuid = group1.uuid
            vmNics = [backendVmNic("backend-vm-2", "50"), backendVmNic("backend-vm-4", "80")]
        }
        to = lastLbTO(listener.uuid)
        assertServerGroups(to, [
                (group1.uuid): [(backendIp1): 100L, (backendIp2): 50L, (backendIp4): 80L]
        ])

        changeLoadBalancerBackendServer {
            serverGroupUuid = group1.uuid
            vmNics = [backendVmNic("backend-vm-2", "0")]
        }
        to = lastLbTO(listener.uuid)
        assertServerGroups(to, [
                (group1.uuid): [(backendIp1): 100L, (backendIp2): 0L, (backendIp4): 80L]
        ])

        LoadBalancerServerGroupInventory group2 = createServerGroupWithVmNics(
                "tcp-ipvs-t2-group-2",
                [backendVmNic("backend-vm-3", "30")])
        addServerGroupToLoadBalancerListener {
            listenerUuid = listener.uuid
            serverGroupUuid = group2.uuid
        }
        to = lastLbTO(listener.uuid)
        assertServerGroups(to, [
                (group1.uuid): [(backendIp1): 100L, (backendIp2): 0L, (backendIp4): 80L],
                (group2.uuid): [(backendIp3): 30L]
        ])

        [
                LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN,
                LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN,
                LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN,
                LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_SOURCE
        ].each { String algorithm ->
            int offset = refreshCmds.size()
            changeBalancerAlgorithm(listener.uuid, algorithm)
            to = lastLbTO(listener.uuid, offset)
            assertTcpIpvsTO(to, listener.uuid, 11090, algorithm)
        }

        assert Q.New(LoadBalancerVO.class)
                .select(LoadBalancerVO_.providerType)
                .eq(LoadBalancerVO_.uuid, lb.uuid)
                .findValue() == VyosConstants.VYOS_ROUTER_PROVIDER_TYPE

        List<String> applianceVmUuids = Q.New(ApplianceVmVO.class)
                .select(ApplianceVmVO_.uuid)
                .listValues()
        int destroyOffset = destroyVmCmds.size()
        deleteLoadBalancer {
            uuid = lb.uuid
        }
        assert destroyVmCmds.size() > destroyOffset
        assert destroyVmCmds.drop(destroyOffset).any { applianceVmUuids.contains(it.uuid) }
    }

    LoadBalancerListenerInventory createTcpIpvsListener(String name, int port, String algorithm) {
        return createLoadBalancerListener {
            delegate.name = name
            delegate.loadBalancerUuid = lb.uuid
            delegate.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
            delegate.loadBalancerPort = port
            delegate.instancePort = 8080
            delegate.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
            delegate.forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
            delegate.systemTags = ["balancerAlgorithm::${algorithm}".toString()]
        }
    }

    Map<String, String> backendVmNic(String vmName, String weight) {
        return ["uuid": backendVmNicUuid(vmName), "weight": weight]
    }

    String backendVmNicUuid(String vmName) {
        return (env.inventoryByName(vmName) as VmInstanceInventory).vmNics[0].uuid
    }

    String backendIp(String vmName) {
        return (env.inventoryByName(vmName) as VmInstanceInventory).vmNics[0].ip
    }

    LoadBalancerServerGroupInventory createServerGroupWithVmNics(String groupName, List<Map<String, String>> backendNics) {
        LoadBalancerServerGroupInventory group = createLoadBalancerServerGroup {
            loadBalancerUuid = lb.uuid
            name = groupName
        }
        addBackendServerToServerGroup {
            serverGroupUuid = group.uuid
            vmNics = backendNics
        }
        return group
    }

    VirtualRouterLoadBalancerBackend.LbTO lastLbTO(String listenerUuid, int offset = 0) {
        VirtualRouterLoadBalancerBackend.LbTO to = null
        retryInSecs {
            assert refreshCmds.size() > offset
            to = refreshCmds.drop(offset).reverse()
                    .collectMany { it.lbs }
                    .find { it.listenerUuid == listenerUuid }
            assert to != null
        }
        return to
    }

    void assertTcpIpvsTO(VirtualRouterLoadBalancerBackend.LbTO to, String listenerUuid, int port, String algorithm) {
        assert to.listenerUuid == listenerUuid
        assert to.mode == LoadBalancerConstants.LB_PROTOCOL_TCP
        assert to.dataPlane == LoadBalancerConstants.DATA_PLANE_IPVS
        assert to.forwardMode == LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        assert to.loadBalancerPort == port
        assert to.instancePort == 8080
        assert to.parameters.contains("balancerAlgorithm::${algorithm}".toString())
    }

    void assertServerGroups(VirtualRouterLoadBalancerBackend.LbTO to, Map<String, Map<String, Long>> expected) {
        assert to.serverGroups.size() == expected.size()
        expected.each { String groupUuid, Map<String, Long> servers ->
            def group = to.serverGroups.find { it.serverGroupUuid == groupUuid }
            assert group != null
            assert group.backendServers.size() == servers.size()
            servers.each { String ip, Long weight ->
                def server = group.backendServers.find { it.ip == ip }
                assert server != null
                assert server.weight == weight
                assert to.nicIps.contains(ip)
                assert to.parameters.contains("balancerWeight::${ip}::${weight}".toString())
            }
        }
    }

    void changeBalancerAlgorithm(String listenerUuid, String algorithm) {
        ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction()
        action.uuid = listenerUuid
        action.balancerAlgorithm = algorithm
        action.sessionId = adminSession()
        ChangeLoadBalancerListenerAction.Result result = action.call()
        assert result.error == null
    }

    CreateLoadBalancerListenerAction.Result assertCreateListenerError(int port, String protocol, String dataPlane, String forwardMode) {
        CreateLoadBalancerListenerAction action = new CreateLoadBalancerListenerAction()
        action.name = "tcp-ipvs-invalid-${port}"
        action.loadBalancerUuid = lb.uuid
        action.protocol = protocol
        action.loadBalancerPort = port
        action.instancePort = 8080
        action.dataPlane = dataPlane
        action.forwardMode = forwardMode
        action.sessionId = adminSession()

        CreateLoadBalancerListenerAction.Result result = action.call()
        assert result.error != null
        return result
    }

    String getApiResultString(ApiResult result) {
        def field = ApiResult.class.getDeclaredField("resultString")
        field.setAccessible(true)
        return field.get(result) as String
    }

    @Override
    void clean() {
        env.delete()
    }
}
