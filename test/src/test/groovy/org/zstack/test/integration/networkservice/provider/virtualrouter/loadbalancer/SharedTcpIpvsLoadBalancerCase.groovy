package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.LoadBalancerServerGroupInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.CopyOnWriteArrayList

class SharedTcpIpvsLoadBalancerCase extends SubCase {
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
                        name = "guestL3"
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
                            startIp = "10.2.226.10"
                            endIp = "10.2.226.200"
                            gateway = "10.2.226.1"
                            netmask = "255.255.255.0"
                        }
                    }

                    l3Network {
                        name = "publicL3"
                        ip {
                            startIp = "172.24.3.10"
                            endIp = "172.24.3.200"
                            gateway = "172.24.3.1"
                            netmask = "255.255.255.0"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("publicL3")
                    usePublicL3Network("publicL3")
                    useImage("vr")
                    isDefault = true
                }
            }

            vm {
                name = "backend-vm"
                useImage("image")
                useL3Networks("guestL3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testSharedTcpIpvsRefreshPayload()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testSharedTcpIpvsRefreshPayload() {
        L3NetworkInventory publicL3 = env.inventoryByName("publicL3") as L3NetworkInventory
        VmInstanceInventory vm = env.inventoryByName("backend-vm") as VmInstanceInventory
        VmNicInventory vmNic = vm.vmNics[0]

        VipInventory vip = createVip {
            name = "shared-tcp-ipvs-vip"
            l3NetworkUuid = publicL3.uuid
        }

        LoadBalancerInventory lb = createLoadBalancer {
            name = "shared-tcp-ipvs-lb"
            vipUuid = vip.uuid
        }

        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            name = "shared-tcp-ipvs-listener"
            loadBalancerUuid = lb.uuid
            protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
            loadBalancerPort = 19094
            instancePort = 8080
            dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
            forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        }

        LoadBalancerServerGroupInventory sg = createLoadBalancerServerGroup {
            loadBalancerUuid = lb.uuid
            name = "shared-tcp-ipvs-server-group"
        }

        addBackendServerToServerGroup {
            vmNics = [['uuid': vmNic.uuid, 'weight': '100']]
            serverGroupUuid = sg.uuid
        }

        List<VirtualRouterLoadBalancerBackend.RefreshLbCmd> refreshCmds = new CopyOnWriteArrayList<>()
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            refreshCmds.add(JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class))
            return rsp
        }

        int offset = refreshCmds.size()
        addServerGroupToLoadBalancerListener {
            listenerUuid = listener.uuid
            serverGroupUuid = sg.uuid
        }

        VirtualRouterLoadBalancerBackend.LbTO to = null
        retryInSecs {
            assert refreshCmds.size() > offset
            to = refreshCmds.drop(offset).collectMany { it.lbs }
                    .find { it.listenerUuid == listener.uuid }
            assert to != null
        }

        assert to.lbUuid == lb.uuid
        assert to.listenerUuid == listener.uuid
        assert to.mode == LoadBalancerConstants.LB_PROTOCOL_TCP
        assert to.dataPlane == LoadBalancerConstants.DATA_PLANE_IPVS
        assert to.forwardMode == LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        assert to.vip == vip.ip
        assert to.loadBalancerPort == 19094
        assert to.instancePort == 8080
        assert to.nicIps.contains(vmNic.ip)
        assert to.serverGroups.size() == 1
        assert to.serverGroups[0].serverGroupUuid == sg.uuid
        assert to.serverGroups[0].backendServers.size() == 1
        assert to.serverGroups[0].backendServers[0].ip == vmNic.ip
        assert to.serverGroups[0].backendServers[0].weight == 100
    }
}
