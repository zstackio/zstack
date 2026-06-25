package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerListenerVO
import org.zstack.network.service.lb.LoadBalancerListenerVO_
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.ApiResult
import org.zstack.sdk.CreateLoadBalancerListenerAction
import org.zstack.sdk.CreateLoadBalancerListenerResult
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.LoadBalancerListenerInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.ZSClient
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static java.util.Arrays.asList

class TcpIpvsLoadBalancerListenerApiCase extends SubCase {
    EnvSpec env
    LoadBalancerInventory lb

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
            delegate.systemTags = asList("separateVirtualRouterVm")
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
