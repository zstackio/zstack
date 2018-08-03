package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.network.service.lb.LoadBalancerVO_
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerBackend
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by heathhose on 17-5-5.
 */
class VirtualRouterLoadBalancerUDPCase extends SubCase{
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
                        protocol = "udp"
                        loadBalancerPort = 10000
                        instancePort = 10000
                        useVmNic("vm", "l3")
                        useVmNic("vm2", "l3")
                        systemTags = ["healthCheckTarget::udp:10000"]
                    }
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
            TestCreateLoadBalancerListener()

            TestVirtualRouterDownReconnectVm()
            //TestUpdateLoadBalancerCase()
            //TestDeleteLoadBalancerListener()
        }
    }

    void TestCreateLoadBalancerListener() {
        L3NetworkInventory pubL3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        LoadBalancerInventory lb = env.inventoryByName("lb")
        VipVO vip = dbFindByUuid(lb.getVipUuid(),VipVO.class)


        LoadBalancerListenerInventory listener = createLoadBalancerListener {
            protocol = "udp"
            loadBalancerUuid = lb.uuid
            loadBalancerPort = 44
            instancePort = 22
            name = "test-listener"
            systemTags = ["healthCheckTarget::udp:22"]
        }

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        addVmNicToLoadBalancer {
            listenerUuid = listener.uuid
            vmNicUuids = [vm.getVmNics().get(0).getUuid()]
        }

        assert cmd.lbs.get(0).mode == LoadBalancerConstants.LB_PROTOCOL_UDP

        deleteLoadBalancerListener {
            uuid = listener.uuid
        }
    }

    void TestVirtualRouterDownReconnectVm(){
        // test a lb with multiple listeners, once the vr destroyed and recreated,
        // database has only one VirtualRouterLoadBalancerRefVO for the lb and vr
        assert dbf.count(VirtualRouterLoadBalancerRefVO.class) == 1
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0)
        VmInstanceInventory vm = env.inventoryByName("vm")
        LoadBalancerInventory load = env.inventoryByName("lb")
        destroyVmInstance {
            uuid = vr.uuid
        }

        assert dbf.count(VirtualRouterVmVO.class) == 0
        assert dbf.count(VirtualRouterLoadBalancerRefVO.class) == 0

        rebootVmInstance {
            uuid = vm.uuid
        }

        List<VirtualRouterLoadBalancerRefVO> list = dbf.listAll(VirtualRouterLoadBalancerRefVO.class)
        assert list.size() == 1
        assert list.get(0).getLoadBalancerUuid() == load.getUuid()
        LoadBalancerVO lb = dbFindByUuid(list.get(0).getLoadBalancerUuid(),LoadBalancerVO.class)
        assert !lb.getListeners().isEmpty()
        assert LoadBalancerConstants.LB_PROTOCOL_UDP == lb.getListeners().toArray()[0].getProtocol()
    }

    void TestUpdateLoadBalancerCase() {
        LoadBalancerInventory load = env.inventoryByName("lb")
        def _name = "test2"
        def _description = "info2"
        LoadBalancerVO lbvo = Q.New(LoadBalancerVO.class).eq(LoadBalancerVO_.uuid, load.uuid).find()
        def lbUuid = lbvo.uuid

        UpdateLoadBalancerAction updateLoadBalancerAction = new UpdateLoadBalancerAction()
        updateLoadBalancerAction.uuid  = lbUuid
        updateLoadBalancerAction.name = _name
        updateLoadBalancerAction.description = _description
        updateLoadBalancerAction.sessionId = adminSession()
        UpdateLoadBalancerAction.Result res = updateLoadBalancerAction.call()
        assert res.error == null
        assert res.value.inventory.name == _name
        assert res.value.inventory.description == _description
        LoadBalancerVO lbvo2 = Q.New(LoadBalancerVO.class).eq(LoadBalancerVO_.uuid, load.uuid).find()
        assert lbvo2.name == _name
        assert lbvo2.description == _description
    }
    @Override
    void clean() {
        env.delete()
    }

}
