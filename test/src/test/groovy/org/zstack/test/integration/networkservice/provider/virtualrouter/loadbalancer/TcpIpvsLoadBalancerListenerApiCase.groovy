package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.appliancevm.ApplianceVmVO_
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerAclStatus
import org.zstack.network.service.lb.LoadBalancerAclType
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerListenerVO
import org.zstack.network.service.lb.LoadBalancerListenerVO_
import org.zstack.network.service.lb.LoadBalancerSystemTags
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.network.service.lb.LoadBalancerVO_
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.network.service.virtualrouter.vyos.VyosGlobalConfig
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.sdk.AccessControlListInventory
import org.zstack.sdk.AddBackendServerToServerGroupAction
import org.zstack.sdk.AddServerGroupToLoadBalancerListenerAction
import org.zstack.sdk.ChangeLoadBalancerListenerAction
import org.zstack.sdk.ApiResult
import org.zstack.sdk.CreateLoadBalancerListenerAction
import org.zstack.sdk.CreateLoadBalancerListenerResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.LoadBalancerServerGroupInventory
import org.zstack.sdk.QueryLoadBalancerListenerAction
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ZSClient
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants
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
            installRefreshLbCapture()
            testTcpIpvsIpv6Validation()
            testTcpHaproxyDefaultDataPlane()
            testTcpIpvsFullNatListener()
            testTcpIpvsSupportedParameterInventory()
            testTcpIpvsNatAndDrListener()
            testTcpIpvsDefaultForwardMode()
            testTcpIpvsForwardModeCannotBeChanged()
            testTcpIpvsCreateValidation()
            testTcpIpvsHealthCheckParameterValidation()
            testTcpHaproxyBackendRefreshPayload()
            testUdpHaproxyBackendRefreshPayload()
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

    void installRefreshLbCapture() {
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            refreshCmds.add(JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class))
            return rsp
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
        assert !rawResult.contains("\"balancerAlgorithm\"")
        assert !rawResult.contains("\"maxConnection\"")

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

    void testTcpIpvsSupportedParameterInventory() {
        CreateLoadBalancerListenerAction createAction = new CreateLoadBalancerListenerAction()
        createAction.name = "tcp-ipvs-supported-parameters"
        createAction.loadBalancerUuid = lb.uuid
        createAction.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        createAction.loadBalancerPort = 11074
        createAction.instancePort = 8080
        createAction.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
        createAction.forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        createAction.systemTags = [
                "balancerAlgorithm::${LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN}".toString(),
                "maxConnection::1234"
        ]
        createAction.sessionId = adminSession()

        ApiResult createResult = ZSClient.call(createAction)
        assert createResult.error == null
        String rawResult = getApiResultString(createResult)
        assert rawResult.contains("\"balancerAlgorithm\":\"${LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN}\"".toString())
        assert rawResult.contains("\"maxConnection\":1234")

        LoadBalancerListenerInventory listener = createResult.getResult(CreateLoadBalancerListenerResult.class).inventory

        assert listener.dataPlane == LoadBalancerConstants.DATA_PLANE_IPVS
        assert listener.forwardMode == LoadBalancerConstants.FORWARD_MODE_FULL_NAT

        rawResult = queryListenerRaw(listener.uuid)
        assert rawResult.contains("\"balancerAlgorithm\":\"${LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN}\"".toString())
        assert rawResult.contains("\"maxConnection\":1234")

        String backendIp = backendIp("backend-vm-1")
        LoadBalancerServerGroupInventory group = createServerGroupWithVmNics(
                "tcp-ipvs-supported-parameters-group",
                [backendVmNic("backend-vm-1", "100")])

        int refreshOffset = refreshCmds.size()
        addServerGroupToLoadBalancerListener {
            listenerUuid = listener.uuid
            serverGroupUuid = group.uuid
        }

        VirtualRouterLoadBalancerBackend.LbTO to = lastLbTOWithParameters(listener.uuid, [
                "balancerAlgorithm::${LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN}".toString(),
                "maxConnection::1234"
        ], refreshOffset)
        assertTcpIpvsTO(to, listener.uuid, 11074, LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN)
        assert to.parameters.contains("maxConnection::1234")
        assertServerGroups(to, [
                (group.uuid): [(backendIp): 100L]
        ])

        ChangeLoadBalancerListenerAction changeAction = new ChangeLoadBalancerListenerAction()
        changeAction.uuid = listener.uuid
        changeAction.balancerAlgorithm = LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN
        changeAction.maxConnection = 2345
        changeAction.sessionId = adminSession()

        refreshOffset = refreshCmds.size()
        ApiResult changeResult = ZSClient.call(changeAction)
        assert changeResult.error == null
        rawResult = getApiResultString(changeResult)
        assert rawResult.contains("\"balancerAlgorithm\":\"${LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN}\"".toString())
        assert rawResult.contains("\"maxConnection\":2345")

        to = lastLbTOWithParameters(listener.uuid, [
                "balancerAlgorithm::${LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN}".toString(),
                "maxConnection::2345"
        ], refreshOffset)
        assertTcpIpvsTO(to, listener.uuid, 11074, LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN)
        assert to.parameters.contains("maxConnection::2345")

        rawResult = queryListenerRaw(listener.uuid)
        assert rawResult.contains("\"balancerAlgorithm\":\"${LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN}\"".toString())
        assert rawResult.contains("\"maxConnection\":2345")
    }

    void testTcpIpvsNatAndDrListener() {
        [
                [11076, LoadBalancerConstants.FORWARD_MODE_NAT],
                [11077, LoadBalancerConstants.FORWARD_MODE_DR]
        ].each { List param ->
            LoadBalancerListenerInventory listener = createLoadBalancerListener {
                delegate.name = "tcp-ipvs-${param[1]}"
                delegate.loadBalancerUuid = lb.uuid
                delegate.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
                delegate.loadBalancerPort = param[0] as int
                delegate.instancePort = 8080
                delegate.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
                delegate.forwardMode = param[1] as String
            }

            assert listener.dataPlane == LoadBalancerConstants.DATA_PLANE_IPVS
            assert listener.forwardMode == param[1]

            LoadBalancerListenerVO vo = Q.New(LoadBalancerListenerVO.class)
                    .eq(LoadBalancerListenerVO_.uuid, listener.uuid)
                    .find()
            assert vo.dataPlane == LoadBalancerConstants.DATA_PLANE_IPVS
            assert vo.forwardMode == param[1]
        }
    }

    void testTcpIpvsIpv6Validation() {
        L3NetworkInventory publicL3 = env.inventoryByName("publicL3") as L3NetworkInventory
        addIpv6Range {
            name = "public-ipv6-range"
            l3NetworkUuid = publicL3.uuid
            startIp = "2003:2001::0010"
            endIp = "2003:2001::0020"
            gateway = "2003:2001::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        LoadBalancerServerGroupInventory ipv6Group = createLoadBalancerServerGroup {
            loadBalancerUuid = lb.uuid
            name = "tcp-ipvs-ipv6-group-before-listener"
            ipVersion = IPv6Constants.IPv6
        }

        LoadBalancerListenerInventory listener = createTcpIpvsListener(
                "tcp-ipvs-ipv6-validation", 11079, LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN)

        AddServerGroupToLoadBalancerListenerAction addIpv6Group = new AddServerGroupToLoadBalancerListenerAction()
        addIpv6Group.listenerUuid = listener.uuid
        addIpv6Group.serverGroupUuid = ipv6Group.uuid
        addIpv6Group.sessionId = adminSession()
        AddServerGroupToLoadBalancerListenerAction.Result addIpv6GroupResult = addIpv6Group.call()
        assert addIpv6GroupResult.error != null
        assert addIpv6GroupResult.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10181"
        assert addIpv6GroupResult.error.details.contains("tcp ipvs listener doesn't support ipv6 server group")

        LoadBalancerServerGroupInventory ipv6GroupAfterListener = createLoadBalancerServerGroup {
            loadBalancerUuid = lb.uuid
            name = "tcp-ipvs-ipv6-group-after-listener"
            ipVersion = IPv6Constants.IPv6
        }
        AddServerGroupToLoadBalancerListenerAction addIpv6GroupAfterListener = new AddServerGroupToLoadBalancerListenerAction()
        addIpv6GroupAfterListener.listenerUuid = listener.uuid
        addIpv6GroupAfterListener.serverGroupUuid = ipv6GroupAfterListener.uuid
        addIpv6GroupAfterListener.sessionId = adminSession()
        AddServerGroupToLoadBalancerListenerAction.Result addIpv6GroupAfterListenerResult = addIpv6GroupAfterListener.call()
        assert addIpv6GroupAfterListenerResult.error != null
        assert addIpv6GroupAfterListenerResult.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10181"
        assert addIpv6GroupAfterListenerResult.error.details.contains("tcp ipvs listener doesn't support ipv6 server group")

        LoadBalancerServerGroupInventory ipv4Group = createLoadBalancerServerGroup {
            loadBalancerUuid = lb.uuid
            name = "tcp-ipvs-ipv6-backend-ip-group"
        }
        addServerGroupToLoadBalancerListener {
            listenerUuid = listener.uuid
            serverGroupUuid = ipv4Group.uuid
        }
        AddBackendServerToServerGroupAction addIpv6BackendIp = new AddBackendServerToServerGroupAction()
        addIpv6BackendIp.serverGroupUuid = ipv4Group.uuid
        addIpv6BackendIp.servers = [["ipAddress": "2003:2001::12", "weight": "100"]]
        addIpv6BackendIp.sessionId = adminSession()
        AddBackendServerToServerGroupAction.Result addIpv6BackendIpResult = addIpv6BackendIp.call()
        assert addIpv6BackendIpResult.error != null
        assert addIpv6BackendIpResult.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10182"
        assert addIpv6BackendIpResult.error.details.contains("tcp ipvs listener doesn't support ipv6 backend server ip")

        VipInventory ipv4Vip = createVip {
            name = "tcp-ipvs-dual-stack-ipv4-vip"
            l3NetworkUuid = publicL3.uuid
            ipVersion = IPv6Constants.IPv4
        }
        VipInventory ipv6Vip = createVip {
            name = "tcp-ipvs-dual-stack-ipv6-vip"
            l3NetworkUuid = publicL3.uuid
            ipVersion = IPv6Constants.IPv6
        }
        LoadBalancerInventory dualStackLb = createLoadBalancer {
            name = "tcp-ipvs-dual-stack-lb"
            vipUuid = ipv4Vip.uuid
            ipv6VipUuid = ipv6Vip.uuid
            systemTags = ["separateVirtualRouterVm"]
        }

        CreateLoadBalancerListenerAction action = new CreateLoadBalancerListenerAction()
        action.name = "tcp-ipvs-ipv6-vip-listener"
        action.loadBalancerUuid = dualStackLb.uuid
        action.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        action.loadBalancerPort = 11078
        action.instancePort = 8080
        action.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
        action.forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        action.sessionId = adminSession()
        CreateLoadBalancerListenerAction.Result result = action.call()
        assert result.error != null
        assert result.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10180"
        assert result.error.details.contains("tcp ipvs listener doesn't support ipv6 vip")
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

    void testTcpIpvsForwardModeCannotBeChanged() {
        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            delegate.name = "tcp-ipvs-forward-mode-immutable"
            delegate.loadBalancerUuid = lb.uuid
            delegate.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
            delegate.loadBalancerPort = 11086
            delegate.instancePort = 8080
            delegate.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
            delegate.forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        }

        ChangeLoadBalancerListenerAction.Result result = assertChangeListenerError(listener.uuid) { ChangeLoadBalancerListenerAction action ->
            action.forwardMode = LoadBalancerConstants.FORWARD_MODE_NAT
        }
        assert result.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10188"
        assert result.error.details.contains("forwardMode cannot be changed after load balancer listener is created")

        LoadBalancerListenerVO vo = Q.New(LoadBalancerListenerVO.class)
                .eq(LoadBalancerListenerVO_.uuid, listener.uuid)
                .find()
        assert vo.forwardMode == LoadBalancerConstants.FORWARD_MODE_FULL_NAT
    }

    void testTcpIpvsCreateValidation() {
        [
                [11083, LoadBalancerConstants.LB_PROTOCOL_HTTP],
                [11084, LoadBalancerConstants.LB_PROTOCOL_UDP],
                [11088, LoadBalancerConstants.LB_PROTOCOL_HTTPS]
        ].each { List param ->
            CreateLoadBalancerListenerAction.Result result = assertCreateListenerError(param[0] as int, param[1] as String,
                    LoadBalancerConstants.DATA_PLANE_IPVS, LoadBalancerConstants.FORWARD_MODE_FULL_NAT)
            assert result.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10183"
            assert result.error.details.contains("data plane [ipvs] only supports tcp listener")
        }

        CreateLoadBalancerListenerAction.Result result = assertCreateListenerError(11085, LoadBalancerConstants.LB_PROTOCOL_TCP,
                LoadBalancerConstants.DATA_PLANE_HAPROXY, LoadBalancerConstants.FORWARD_MODE_FULL_NAT)
        assert result.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10186"
        assert result.error.details.contains("forwardMode is only supported when dataPlane is ipvs")
    }

    void testTcpIpvsHealthCheckParameterValidation() {
        [
                { CreateLoadBalancerListenerAction action -> action.healthCheckMethod = "GET" },
                { CreateLoadBalancerListenerAction action -> action.healthCheckURI = "/health" },
                { CreateLoadBalancerListenerAction action -> action.healthCheckHttpCode = "http_2xx" },
                { CreateLoadBalancerListenerAction action -> action.healthCheckProtocol = LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP; action.healthCheckURI = "/health" },
                { CreateLoadBalancerListenerAction action -> action.systemTags = ["healthCheckParameter::GET:/health:http_2xx"] }
        ].eachWithIndex { Closure setter, int index ->
            CreateLoadBalancerListenerAction.Result result = assertCreateTcpIpvsListenerError(11100 + index, setter)
            assert result.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10187"
            assert result.error.details.contains("tcp ipvs listener doesn't support http health check parameters")
        }

        CreateLoadBalancerListenerAction.Result udpHealthCheckResult = assertCreateTcpIpvsListenerError(11105) { CreateLoadBalancerListenerAction action ->
            action.healthCheckProtocol = LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP
        }
        assertUnsupportedHealthCheckError(udpHealthCheckResult.error)

        CreateLoadBalancerListenerAction.Result timeoutResult = assertCreateTcpIpvsListenerError(11106) { CreateLoadBalancerListenerAction action ->
            action.systemTags = ["healthCheckTimeout::1"]
        }
        assertUnsupportedHealthCheckTimeoutError(timeoutResult.error)

        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            delegate.name = "tcp-ipvs-health-check-parameters"
            delegate.loadBalancerUuid = lb.uuid
            delegate.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
            delegate.loadBalancerPort = 11110
            delegate.instancePort = 8080
            delegate.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
            delegate.forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
            delegate.systemTags = [
                    "healthCheckTarget::tcp:default",
                    "healthCheckInterval::2",
                    "healthyThreshold::2",
                    "unhealthyThreshold::2"
            ]
        }

        LoadBalancerServerGroupInventory group = createServerGroupWithVmNics(
                "tcp-ipvs-health-check-group",
                [backendVmNic("backend-vm-1", "100")])
        int createOffset = refreshCmds.size()
        addServerGroupToLoadBalancerListener {
            listenerUuid = listener.uuid
            serverGroupUuid = group.uuid
        }
        VirtualRouterLoadBalancerBackend.LbTO to = lastLbTOWithParameters(listener.uuid, healthCheckParameters([
                healthCheckTarget: "tcp:default",
                healthCheckInterval: "2",
                healthyThreshold: "2",
                unhealthyThreshold: "2"
        ]), createOffset)
        assertTcpIpvsTO(to, listener.uuid, 11110, LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN)
        assertNoHealthCheckTimeout(to)

        int changeOffset = refreshCmds.size()
        assertChangeListenerSuccess(listener.uuid) { ChangeLoadBalancerListenerAction action ->
            action.healthCheckTarget = "8080"
        }
        assert LoadBalancerSystemTags.HEALTH_TARGET.getTokenByResourceUuid(listener.uuid,
                LoadBalancerSystemTags.HEALTH_TARGET_TOKEN) == "tcp:8080"
        assertHealthCheckPayload(listener.uuid, changeOffset, [
                healthCheckTarget: "tcp:8080",
                healthCheckInterval: "2",
                healthyThreshold: "2",
                unhealthyThreshold: "2"
        ])

        changeOffset = refreshCmds.size()
        assertChangeListenerSuccess(listener.uuid) { ChangeLoadBalancerListenerAction action ->
            action.healthCheckInterval = 3
        }
        assert LoadBalancerSystemTags.HEALTH_INTERVAL.getTokenByResourceUuid(listener.uuid,
                LoadBalancerSystemTags.HEALTH_INTERVAL_TOKEN) == "3"
        assertHealthCheckPayload(listener.uuid, changeOffset, [
                healthCheckTarget: "tcp:8080",
                healthCheckInterval: "3",
                healthyThreshold: "2",
                unhealthyThreshold: "2"
        ])

        ChangeLoadBalancerListenerAction.Result timeoutChangeResult = assertChangeListenerError(listener.uuid) { ChangeLoadBalancerListenerAction action ->
            action.healthCheckTimeout = 2
        }
        assertUnsupportedHealthCheckTimeoutError(timeoutChangeResult.error)

        changeOffset = refreshCmds.size()
        assertChangeListenerSuccess(listener.uuid) { ChangeLoadBalancerListenerAction action ->
            action.healthyThreshold = 3
        }
        assert LoadBalancerSystemTags.HEALTHY_THRESHOLD.getTokenByResourceUuid(listener.uuid,
                LoadBalancerSystemTags.HEALTHY_THRESHOLD_TOKEN) == "3"
        assertHealthCheckPayload(listener.uuid, changeOffset, [
                healthCheckTarget: "tcp:8080",
                healthCheckInterval: "3",
                healthyThreshold: "3",
                unhealthyThreshold: "2"
        ])

        changeOffset = refreshCmds.size()
        assertChangeListenerSuccess(listener.uuid) { ChangeLoadBalancerListenerAction action ->
            action.unhealthyThreshold = 3
        }
        assert LoadBalancerSystemTags.UNHEALTHY_THRESHOLD.getTokenByResourceUuid(listener.uuid,
                LoadBalancerSystemTags.UNHEALTHY_THRESHOLD_TOKEN) == "3"
        assertHealthCheckPayload(listener.uuid, changeOffset, [
                healthCheckTarget: "tcp:8080",
                healthCheckInterval: "3",
                healthyThreshold: "3",
                unhealthyThreshold: "3"
        ])

        [
                { ChangeLoadBalancerListenerAction action -> action.healthCheckMethod = "GET" },
                { ChangeLoadBalancerListenerAction action -> action.healthCheckURI = "/health" },
                { ChangeLoadBalancerListenerAction action -> action.healthCheckHttpCode = "http_2xx" },
                { ChangeLoadBalancerListenerAction action -> action.healthCheckProtocol = LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP; action.healthCheckURI = "/health" }
        ].each { Closure setter ->
            ChangeLoadBalancerListenerAction.Result result = assertChangeListenerError(listener.uuid, setter)
            assert result.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10187"
            assert result.error.details.contains("tcp ipvs listener doesn't support http health check parameters")
        }

        ChangeLoadBalancerListenerAction.Result udpChangeResult = assertChangeListenerError(listener.uuid) { ChangeLoadBalancerListenerAction action ->
            action.healthCheckProtocol = LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP
        }
        assertUnsupportedHealthCheckError(udpChangeResult.error)

        ChangeLoadBalancerListenerAction.Result noneSpecificTargetResult = assertChangeListenerError(listener.uuid) { ChangeLoadBalancerListenerAction action ->
            action.healthCheckTarget = "none:8080"
        }
        assert noneSpecificTargetResult.error.details.contains("health check protocol none only supports default target")
    }

    void testTcpHaproxyBackendRefreshPayload() {
        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            delegate.name = "tcp-haproxy-refresh"
            delegate.loadBalancerUuid = lb.uuid
            delegate.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
            delegate.loadBalancerPort = 11120
            delegate.instancePort = 8081
            delegate.systemTags = ["healthCheckTarget::tcp:8081"]
        }

        assert listener.dataPlane == null
        assert listener.forwardMode == null

        LoadBalancerListenerVO vo = Q.New(LoadBalancerListenerVO.class)
                .eq(LoadBalancerListenerVO_.uuid, listener.uuid)
                .find()
        assert vo.dataPlane == LoadBalancerConstants.DATA_PLANE_HAPROXY
        assert vo.forwardMode == null

        String backendIp1 = backendIp("backend-vm-1")
        String backendIp2 = backendIp("backend-vm-2")
        LoadBalancerServerGroupInventory group = createServerGroupWithVmNics(
                "tcp-haproxy-refresh-group",
                [backendVmNic("backend-vm-1", "100"), backendVmNic("backend-vm-2", "60")])

        int offset = refreshCmds.size()
        addServerGroupToLoadBalancerListener {
            listenerUuid = listener.uuid
            serverGroupUuid = group.uuid
        }

        VirtualRouterLoadBalancerBackend.LbTO to = lastLbTOWithParameters(
                listener.uuid, ["healthCheckTarget::tcp:8081"], offset)
        assertHaproxyTO(to, listener.uuid, LoadBalancerConstants.LB_PROTOCOL_TCP, 11120, 8081)
        assertServerGroups(to, [
                (group.uuid): [(backendIp1): 100L, (backendIp2): 60L]
        ])
    }

    void testUdpHaproxyBackendRefreshPayload() {
        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            delegate.name = "udp-haproxy-refresh"
            delegate.loadBalancerUuid = lb.uuid
            delegate.protocol = LoadBalancerConstants.LB_PROTOCOL_UDP
            delegate.loadBalancerPort = 11121
            delegate.instancePort = 8082
            delegate.systemTags = ["healthCheckTarget::udp:8082"]
        }

        assert listener.dataPlane == null
        assert listener.forwardMode == null

        LoadBalancerListenerVO vo = Q.New(LoadBalancerListenerVO.class)
                .eq(LoadBalancerListenerVO_.uuid, listener.uuid)
                .find()
        assert vo.dataPlane == LoadBalancerConstants.DATA_PLANE_HAPROXY
        assert vo.forwardMode == null

        String backendIp3 = backendIp("backend-vm-3")
        String backendIp4 = backendIp("backend-vm-4")
        LoadBalancerServerGroupInventory group = createServerGroupWithVmNics(
                "udp-haproxy-refresh-group",
                [backendVmNic("backend-vm-3", "100"), backendVmNic("backend-vm-4", "70")])

        int offset = refreshCmds.size()
        addServerGroupToLoadBalancerListener {
            listenerUuid = listener.uuid
            serverGroupUuid = group.uuid
        }

        VirtualRouterLoadBalancerBackend.LbTO to = lastLbTOWithParameters(
                listener.uuid, ["healthCheckTarget::udp:8082"], offset)
        assertHaproxyTO(to, listener.uuid, LoadBalancerConstants.LB_PROTOCOL_UDP, 11121, 8082)
        assertServerGroups(to, [
                (group.uuid): [(backendIp3): 100L, (backendIp4): 70L]
        ])
    }

    void testTcpIpvsDedicatedListenerBackendRefreshPayload() {
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
        assert !to.enableFullLog

        testTcpIpvsFullLogPayload(listener, group1)

        addBackendServerToServerGroup {
            serverGroupUuid = group1.uuid
            vmNics = [backendVmNic("backend-vm-2", "50"), backendVmNic("backend-vm-4", "80")]
        }
        to = lastLbTO(listener.uuid)
        assertServerGroups(to, [
                (group1.uuid): [(backendIp1): 100L, (backendIp2): 50L, (backendIp4): 80L]
        ])

        testTcpIpvsWhitelistPayload(group1)

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

    void testTcpIpvsWhitelistPayload(LoadBalancerServerGroupInventory group) {
        AccessControlListInventory acl = createAccessControlList {
            name = "tcp-ipvs-white-acl"
            ipVersion = 4
        }
        addAccessControlListEntry {
            aclUuid = acl.uuid
            entries = "192.168.10.1,192.168.11.0/24"
        }

        LoadBalancerListenerInventory createWithWhitelist = createLoadBalancerListener {
            delegate.name = "tcp-ipvs-white-create"
            delegate.loadBalancerUuid = lb.uuid
            delegate.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
            delegate.loadBalancerPort = 11091
            delegate.instancePort = 8080
            delegate.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
            delegate.forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
            delegate.aclStatus = LoadBalancerAclStatus.enable.toString()
            delegate.aclType = LoadBalancerAclType.white.toString()
            delegate.aclUuids = [acl.uuid]
            delegate.systemTags = ["balancerAlgorithm::${LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN}".toString()]
        }
        assert createWithWhitelist.aclRefs.size() == 1
        assert createWithWhitelist.aclRefs[0].aclUuid == acl.uuid
        assert createWithWhitelist.aclRefs[0].type == LoadBalancerAclType.white.toString()

        int createOffset = refreshCmds.size()
        addServerGroupToLoadBalancerListener {
            listenerUuid = createWithWhitelist.uuid
            serverGroupUuid = group.uuid
        }
        assertWhitelistTO(lastLbTOWithParameters(createWithWhitelist.uuid, whitelistParameters(), createOffset),
                createWithWhitelist.uuid, 11091)

        LoadBalancerListenerInventory changeToWhitelist = createTcpIpvsListener(
                "tcp-ipvs-white-change", 11092, LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN)
        addServerGroupToLoadBalancerListener {
            listenerUuid = changeToWhitelist.uuid
            serverGroupUuid = group.uuid
        }
        addAccessControlListToLoadBalancer {
            listenerUuid = changeToWhitelist.uuid
            aclUuids = [acl.uuid]
            aclType = LoadBalancerAclType.white.toString()
        }

        int changeOffset = refreshCmds.size()
        assertChangeListenerSuccess(changeToWhitelist.uuid) { ChangeLoadBalancerListenerAction action ->
            action.aclStatus = LoadBalancerAclStatus.enable.toString()
        }
        assertWhitelistTO(lastLbTOWithParameters(changeToWhitelist.uuid, whitelistParameters(), changeOffset),
                changeToWhitelist.uuid, 11092)

        deleteAccessControlList {
            uuid = acl.uuid
        }
    }

    void testTcpIpvsFullLogPayload(LoadBalancerListenerInventory listener, LoadBalancerServerGroupInventory group) {
        updateResourceConfig {
            category = VyosGlobalConfig.CATEGORY
            name = VyosGlobalConfig.ENABLE_LOADBALANCER_FULL_LOG.name
            resourceUuid = listener.uuid
            value = "true"
        }

        int enableOffset = refreshCmds.size()
        changeBalancerAlgorithm(listener.uuid, LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN)
        VirtualRouterLoadBalancerBackend.LbTO to = lastLbTO(listener.uuid, enableOffset)
        assertTcpIpvsTO(to, listener.uuid, 11090, LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN)
        assert to.enableFullLog

        LoadBalancerListenerInventory disabledFullLogListener = createTcpIpvsListener(
                "tcp-ipvs-full-log-disabled", 11093, LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN)
        addServerGroupToLoadBalancerListener {
            listenerUuid = disabledFullLogListener.uuid
            serverGroupUuid = group.uuid
        }
        to = lastLbTO(disabledFullLogListener.uuid)
        assertTcpIpvsTO(to, disabledFullLogListener.uuid, 11093, LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN)
        assert !to.enableFullLog

        updateResourceConfig {
            category = VyosGlobalConfig.CATEGORY
            name = VyosGlobalConfig.ENABLE_LOADBALANCER_FULL_LOG.name
            resourceUuid = listener.uuid
            value = "false"
        }

        int disableOffset = refreshCmds.size()
        changeBalancerAlgorithm(listener.uuid, LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN)
        to = lastLbTO(listener.uuid, disableOffset)
        assertTcpIpvsTO(to, listener.uuid, 11090, LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN)
        assert !to.enableFullLog
    }

    void assertHealthCheckPayload(String listenerUuid, int offset, Map<String, String> expected) {
        VirtualRouterLoadBalancerBackend.LbTO to = lastLbTOWithParameters(listenerUuid, healthCheckParameters(expected), offset)
        healthCheckParameters(expected).each { String parameter ->
            assert to.parameters.contains(parameter)
        }
        assertNoHealthCheckTimeout(to)
    }

    List<String> healthCheckParameters(Map<String, String> values) {
        return [
                "healthCheckTarget::${values.healthCheckTarget}".toString(),
                "healthCheckInterval::${values.healthCheckInterval}".toString(),
                "healthyThreshold::${values.healthyThreshold}".toString(),
                "unhealthyThreshold::${values.unhealthyThreshold}".toString()
        ]
    }

    void assertNoHealthCheckTimeout(VirtualRouterLoadBalancerBackend.LbTO to) {
        assert !to.parameters.any { String parameter -> parameter.startsWith("healthCheckTimeout::") }
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

    VirtualRouterLoadBalancerBackend.LbTO lastLbTOWithParameters(String listenerUuid, List<String> parameters, int offset = 0) {
        VirtualRouterLoadBalancerBackend.LbTO to = null
        retryInSecs {
            assert refreshCmds.size() > offset
            to = refreshCmds.drop(offset).reverse()
                    .collectMany { it.lbs }
                    .find { it.listenerUuid == listenerUuid && parameters.every { p -> it.parameters.contains(p) } }
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

    void assertHaproxyTO(VirtualRouterLoadBalancerBackend.LbTO to, String listenerUuid, String protocol,
                         int loadBalancerPort, int instancePort) {
        assert to.listenerUuid == listenerUuid
        assert to.mode == protocol
        assert to.dataPlane == null
        assert to.forwardMode == null
        assert to.loadBalancerPort == loadBalancerPort
        assert to.instancePort == instancePort
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

    void assertWhitelistTO(VirtualRouterLoadBalancerBackend.LbTO to, String listenerUuid, int port) {
        assertTcpIpvsTO(to, listenerUuid, port, LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN)
        whitelistParameters().each { String parameter ->
            assert to.parameters.contains(parameter)
        }
    }

    List<String> whitelistParameters() {
        return [
                "accessControlStatus::enable",
                "aclType::${LoadBalancerAclType.white}".toString(),
                "aclEntry::192.168.10.1,192.168.11.0/24"
        ]
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

    CreateLoadBalancerListenerAction.Result assertCreateTcpIpvsListenerError(int port, Closure setter) {
        CreateLoadBalancerListenerAction action = new CreateLoadBalancerListenerAction()
        action.name = "tcp-ipvs-invalid-health-check-${port}"
        action.loadBalancerUuid = lb.uuid
        action.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        action.loadBalancerPort = port
        action.instancePort = 8080
        action.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
        action.forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        action.sessionId = adminSession()

        setter.call(action)

        CreateLoadBalancerListenerAction.Result result = action.call()
        assert result.error != null
        return result
    }

    ChangeLoadBalancerListenerAction.Result assertChangeListenerSuccess(String listenerUuid, Closure setter) {
        ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction()
        action.uuid = listenerUuid
        action.sessionId = adminSession()

        setter.call(action)

        ChangeLoadBalancerListenerAction.Result result = action.call()
        assert result.error == null
        return result
    }

    ChangeLoadBalancerListenerAction.Result assertChangeListenerError(String listenerUuid, Closure setter) {
        ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction()
        action.uuid = listenerUuid
        action.sessionId = adminSession()

        setter.call(action)

        ChangeLoadBalancerListenerAction.Result result = action.call()
        assert result.error != null
        return result
    }

    void assertUnsupportedHealthCheckError(def error) {
        assert error.details.contains("doesn't support this health check") ||
                error.details.contains("不支持此类型")
    }

    void assertUnsupportedHealthCheckTimeoutError(def error) {
        assert error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10189"
        assert error.details.contains("tcp ipvs listener doesn't support healthCheckTimeout")
    }

    String getApiResultString(ApiResult result) {
        def field = ApiResult.class.getDeclaredField("resultString")
        field.setAccessible(true)
        return field.get(result) as String
    }

    String queryListenerRaw(String listenerUuid) {
        QueryLoadBalancerListenerAction action = new QueryLoadBalancerListenerAction()
        action.conditions = ["uuid=${listenerUuid}".toString()]
        action.sessionId = adminSession()

        ApiResult result = ZSClient.call(action)
        assert result.error == null
        return getApiResultString(result)
    }

    @Override
    void clean() {
        env.delete()
    }
}
