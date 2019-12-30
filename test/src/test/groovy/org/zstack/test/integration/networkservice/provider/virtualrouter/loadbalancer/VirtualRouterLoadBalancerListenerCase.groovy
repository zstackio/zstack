package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerGlobalConfig
import org.zstack.network.service.lb.LoadBalancerSystemTags
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.ChangeLoadBalancerListenerAction
import org.zstack.sdk.CreateLoadBalancerListenerAction
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
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

                    listener {
                        protocol = "tcp"
                        loadBalancerPort = 22
                        instancePort = 22
                        useVmNic("vm", "l3")
                    }

                    listener {
                        name = "tcp33"
                        protocol = "tcp"
                        loadBalancerPort = 33
                        instancePort = 33
                        useVmNic("vm", "l3")
                    }
                }
            }

            vm {
                name = "vm"
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
            testLoadBalancerWrrCase()
            testLoadBalancerHealthCheckCase()
        }
    }

    void testLoadBalancerHealthCheckCase() {
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def _name = "test5"

        CreateLoadBalancerListenerAction listenerAction = new CreateLoadBalancerListenerAction()
        listenerAction.loadBalancerUuid = load.uuid
        listenerAction.name = _name
        listenerAction.loadBalancerPort = 55
        listenerAction.instancePort = 55
        listenerAction.protocol = "tcp"
        listenerAction.healthCheckProtocol = "http"
        listenerAction.healthCheckMethod = "GET"
        listenerAction.healthCheckURI = "/health.html"
        listenerAction.sessionId = adminSession()

        CreateLoadBalancerListenerAction.Result lblRes = listenerAction.call()
        assert lblRes.error == null

        List<Map<String, String>> tokens = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);

        for (Map<String, String>  token: tokens) {
            assert token.get(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN) == "GET:/health.html:http_2xx"
        }

        ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction()
        action.uuid  = lblRes.value.inventory.uuid
        action.healthCheckURI = "/abc.html"
        action.sessionId = adminSession()
        ChangeLoadBalancerListenerAction.Result res = action.call()
        assert res.error == null
        tokens = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);

        for (Map<String, String>  token: tokens) {
            assert token.get(LoadBalancerSystemTags.HEALTH_PARAMETER_TOKEN) == "GET:/abc.html:http_2xx"
        }

        action.healthCheckProtocol = "tcp"
        res = action.call()
        assert res.error == null
        //tokens = LoadBalancerSystemTags.HEALTH_PARAMETER.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);
        //assert tokens == null || tokens.isEmpty()
    }


    void testLoadBalancerWrrCase() {
        def load = env.inventoryByName("lb") as LoadBalancerInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory
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

        addVmNicToLoadBalancer {
            vmNicUuids = [vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid]
            listenerUuid = lblRes.value.inventory.uuid
        }

        List<Map<String, String>> tokens = LoadBalancerSystemTags.BALANCER_WEIGHT.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);

        for (Map<String, String>  token: tokens) {
            if (!vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid.equals(token.get(LoadBalancerSystemTags.BALANCER_NIC_TOKEN))) {
                continue
            }
            assert token.get(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN) == LoadBalancerGlobalConfig.BALANCER_WEIGHT.value(String.class)
        }

        String weight = "balancerWeight::" + vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid + "::120"
        ChangeLoadBalancerListenerAction action = new ChangeLoadBalancerListenerAction()
        action.uuid  = lblRes.value.inventory.uuid
        action.systemTags = [weight]
        action.sessionId = adminSession()
        ChangeLoadBalancerListenerAction.Result res = action.call()
        assert res.error == null
        tokens = LoadBalancerSystemTags.BALANCER_WEIGHT.getTokensOfTagsByResourceUuid(lblRes.value.inventory.uuid);

        for (Map<String, String>  token: tokens) {
            if (!vm.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid }.uuid.equals(token.get(LoadBalancerSystemTags.BALANCER_NIC_TOKEN))) {
                continue
            }
            assert token.get(LoadBalancerSystemTags.BALANCER_WEIGHT_TOKEN) == "120"
        }

    }

    @Override
    void clean() {
        env.delete()
    }

}
