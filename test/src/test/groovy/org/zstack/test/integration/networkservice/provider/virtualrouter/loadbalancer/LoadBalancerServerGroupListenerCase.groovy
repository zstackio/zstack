package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class LoadBalancerServerGroupListenerCase extends SubCase{
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
                        name = "listener-22"
                        protocol = "tcp"
                        loadBalancerPort = 22
                        instancePort = 22
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
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        env.create {
            TestAddVmNicThenAttachListener()
        }
    }

    void TestAddVmNicThenAttachListener(){
        VmInstanceInventory vm1 = env.inventoryByName("vm-1")
        VmInstanceInventory vm2 = env.inventoryByName("vm-2")
        LoadBalancerListenerInventory lb22 = env.inventoryByName("listener-22")
        def lb = env.inventoryByName("lb") as LoadBalancerInventory

        LoadBalancerServerGroupInventory sg = createLoadBalancerServerGroup{
            loadBalancerUuid =  lb.uuid
            name = "lb-group-1"
        }

        VirtualRouterLoadBalancerBackend.RefreshLbCmd refreshLbCmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            refreshLbCmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        VmNicInventory nic1 = vm1.vmNics.get(0)
        VmNicInventory nic2 = vm2.vmNics.get(0)
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'20'], ['uuid':nic2.uuid,weight:'30']]
            serverGroupUuid = sg.uuid
        }
        assert refreshLbCmd == null

        addServerGroupToLoadBalancerListener {
            listenerUuid = lb22.uuid
            serverGroupUuid = sg.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 1
        VirtualRouterLoadBalancerBackend.LbTO to = refreshLbCmd.lbs.get(0)
        assert to.nicIps.size() == 2
        for (String ip : to.nicIps) {
            assert nic1.ip == ip || nic2.ip == ip
        }

        refreshLbCmd = null
        removeBackendServerFromServerGroup {
            serverGroupUuid = sg.uuid
            vmNicUuids = [nic1.uuid]
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 1
        to = refreshLbCmd.lbs.get(0)
        assert to.nicIps.size() == 1
        assert to.nicIps.get(0) == nic2.ip

        refreshLbCmd = null
        addBackendServerToServerGroup {
            vmNics = [['uuid':nic1.uuid,'weight':'20']]
            serverGroupUuid = sg.uuid
        }
        assert refreshLbCmd != null
        assert refreshLbCmd.lbs.size() == 1
        to = refreshLbCmd.lbs.get(0)
        assert to.nicIps.size() == 2
        for (String ip : to.nicIps) {
            assert nic1.ip == ip || nic2.ip == ip
        }
    }

    @Override
    void clean() {
        env.delete()
    }


}