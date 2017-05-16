package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.ApplianceVmInventory
import org.zstack.sdk.GetCpuMemoryCapacityAction
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.ReconnectVirtualRouterAction
import org.zstack.sdk.ReconnectVirtualRouterResult
import org.zstack.sdk.VipInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by AlanJager on 2017/4/23.
 */
class LoadBalancerCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

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
        env.create {
            testReconnectVirtualRouterCreatingLoaderBalancer()
        }
    }

    void testReconnectVirtualRouterCreatingLoaderBalancer() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        VirtualRouterVmVO vr = Q.New(VirtualRouterVmVO.class).notNull(VirtualRouterVmVO_.uuid).list().get(0)
        def lb = null

        int num = 5
        final CountDownLatch latch = new CountDownLatch(num)

        for (int i = 0; i < 5; i++) {
            try {
                VipInventory vip = createVip {
                    name = "vip"
                    l3NetworkUuid = l3.uuid
                }

                new Thread(new Runnable() {
                    @Override
                    void run() {
                        lb = createLoadBalancer {
                            name = "lb"
                            vipUuid = vip.uuid
                        }
                    }
                }).start()

                reconnectVirtualRouter {
                    vmInstanceUuid = vr.getUuid()
                }
            } catch (Throwable t) {

            } finally {
                latch.countDown()
            }
        }

        latch.await(60, TimeUnit.SECONDS)

        retryInSecs {
            vr = Q.New(VirtualRouterVmVO.class).notNull(VirtualRouterVmVO_.uuid).list().get(0)
            return {
                assert vr.state == VmInstanceState.Running
            }
        }
    }
}
