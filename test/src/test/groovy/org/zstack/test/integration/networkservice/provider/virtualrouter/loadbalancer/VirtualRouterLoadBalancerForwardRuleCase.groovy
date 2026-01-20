package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.acl.AclEntryType
import org.zstack.header.acl.AccessControlListEntryVO
import org.zstack.header.acl.AccessControlListEntryVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerAclStatus
import org.zstack.network.service.lb.LoadBalancerAclType
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerGlobalProperty
import org.zstack.network.service.lb.LoadBalancerListenerACLRefVO
import org.zstack.network.service.lb.LoadBalancerListenerServerGroupRefVO
import org.zstack.network.service.lb.LoadBalancerListenerServerGroupRefVO_
import org.zstack.network.service.lb.LoadBalancerListenerVO
import org.zstack.network.service.lb.LoadBalancerListenerVO_
import org.zstack.network.service.lb.LoadBalancerSystemTags
import org.zstack.network.service.lb.LoadBalancerListenerACLRefVO_
import org.zstack.network.service.lb.LoadBalancerManagerImpl
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

class VirtualRouterLoadBalancerForwardRuleCase extends SubCase {
    DatabaseFacade dbf
    EnvSpec env
    LoadBalancerManagerImpl lbMgr

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
                    name = "lb1"
                    useVip("pubL3")
                }

                lb {
                    name = "lb2"
                    useVip("pubL3")
                }
            }

            vm {
                name = "vm1"
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
        lbMgr = bean(LoadBalancerManagerImpl.class)
        env.create {
            testLoadBalancerForwardRule()
            LoadBalancerGlobalProperty.UPGRADE_LB_REDIRECT_RULE = true
            testUpgradeLoadBalancerForwardRule()
            LoadBalancerGlobalProperty.UPGRADE_LB_REDIRECT_RULE = false
            testLoadBalancerSpecialPortForwardRule()
        }
    }

    void testLoadBalancerForwardRule() {
        def lb1 = env.inventoryByName("lb1") as LoadBalancerInventory
        def vm1 = env.inventoryByName("vm1") as VmInstanceInventory
        def vm2 = env.inventoryByName("vm2") as VmInstanceInventory
        def vm3 = env.inventoryByName("vm3") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        def listener_1 = createLoadBalancerListener {
            name = "listener-1"
            loadBalancerUuid = lb1.uuid
            protocol = "http"
            loadBalancerPort = 80
            instancePort = 8080
        } as LoadBalancerListenerInventory

        def serverGroup_1 = createLoadBalancerServerGroup {
            name = "server-group-1"
            loadBalancerUuid = lb1.uuid
        } as LoadBalancerServerGroupInventory

        addServerGroupToLoadBalancerListener {
            serverGroupUuid = serverGroup_1.uuid
            listenerUuid = listener_1.uuid
        }

        def vmNic1 = vm1.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid } as VmNicInventory
        assert vmNic1 != null

        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        def acl1 = createAccessControlList {
            name = "acl-1"
        } as AccessControlListInventory

        def aclRule1 = addAccessControlListRedirectRule {
            name = "acl-rule-1"
            aclUuid = acl1.uuid
            domain = "zstack.local"
            url = "/test1"
        } as AccessControlListEntryInventory

        def rvo = Q.New(AccessControlListEntryVO.class)
                .eq(AccessControlListEntryVO_.uuid, aclRule1.uuid)
                .find()
        assert rvo != null
        assert rvo.getRedirectPort() == null

        addAccessControlListToLoadBalancer {
            listenerUuid = listener_1.uuid
            serverGroupUuids = [serverGroup_1.uuid]
            aclUuids = [acl1.uuid]
            aclType = "redirect"
        }

        assert Q.New(AccessControlListEntryVO.class)
                .eq(AccessControlListEntryVO_.uuid, aclRule1.uuid)
                .eq(AccessControlListEntryVO_.redirectPort, 8080)
                .isExists()
        
        addBackendServerToServerGroup {
            serverGroupUuid = serverGroup_1.uuid
            vmNics = [[
                uuid: vmNic1.uuid,
                ipVersion: "4"
            ]]
        }
        assert cmd != null
        def lbTO =  cmd.lbs.find { it.lbUuid == lb1.uuid }
        assert lbTO != null
        def redirectRuleTO = lbTO.redirectRules.find { it.redirectRuleUuid == aclRule1.uuid }
        assert redirectRuleTO != null
        assert redirectRuleTO.redirectPort == 8080

        cmd = null
        def acl2 = createAccessControlList {
            name = "acl-2"
        } as AccessControlListInventory
        def aclRule2 = addAccessControlListRedirectRule {
            name = "acl-rule-2"
            aclUuid = acl2.uuid
            domain = "zstack.local"
            url = "/test2"
            redirectPort = 9090
        } as AccessControlListEntryInventory

        assert Q.New(AccessControlListEntryVO.class)
                .eq(AccessControlListEntryVO_.uuid, aclRule2.uuid)
                .eq(AccessControlListEntryVO_.redirectPort, 9090)
                .isExists()

        addAccessControlListToLoadBalancer {
            listenerUuid = listener_1.uuid
            serverGroupUuids = [serverGroup_1.uuid]
            aclUuids = [acl2.uuid]
            aclType = "redirect"
        }
        
        assert cmd != null
        lbTO =  cmd.lbs.find { it.lbUuid == lb1.uuid }
        assert lbTO != null
        redirectRuleTO = lbTO.redirectRules.find { it.redirectRuleUuid == aclRule2.uuid }
        assert redirectRuleTO != null
        assert redirectRuleTO.redirectPort == 9090
    }

    void testLoadBalancerSpecialPortForwardRule() {
        // port 80 is a special port, test whether the redirect rule with port 80 works well
        def lb2 = env.inventoryByName("lb2") as LoadBalancerInventory
        def vm3 = env.inventoryByName("vm3") as VmInstanceInventory
        def l3 = env.inventoryByName("l3") as L3NetworkInventory

        def listener_2 = createLoadBalancerListener {
            name = "listener-2"
            loadBalancerUuid = lb2.uuid
            protocol = "http"
            loadBalancerPort = 8081
            instancePort = 80
        } as LoadBalancerListenerInventory
        def serverGroup_2 = createLoadBalancerServerGroup {
            name = "server-group-2"
            loadBalancerUuid = lb2.uuid
        } as LoadBalancerServerGroupInventory
        addServerGroupToLoadBalancerListener {
            serverGroupUuid = serverGroup_2.uuid
            listenerUuid = listener_2.uuid
        }
        def vmNic3 = vm3.vmNics.find{ nic -> nic.l3NetworkUuid == l3.uuid } as VmNicInventory
        assert vmNic3 != null
        VirtualRouterLoadBalancerBackend.RefreshLbCmd cmd = null
        env.afterSimulator(VirtualRouterLoadBalancerBackend.REFRESH_LB_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, VirtualRouterLoadBalancerBackend.RefreshLbCmd.class)
            return rsp
        }

        def acl3 = createAccessControlList {
            name = "acl-3"
        } as AccessControlListInventory
        def aclRule3 = addAccessControlListRedirectRule {
            name = "acl-rule-3"
            aclUuid = acl3.uuid
            domain = "zstack.local"
            url = "/test3"
            redirectPort = 2000
        } as AccessControlListEntryInventory
        addAccessControlListToLoadBalancer {
            listenerUuid = listener_2.uuid
            serverGroupUuids = [serverGroup_2.uuid]
            aclUuids = [acl3.uuid]
            aclType = "redirect"
        }

        addBackendServerToServerGroup {
            serverGroupUuid = serverGroup_2.uuid
            vmNics = [[
                uuid: vmNic3.uuid,
                ipVersion: "4"
            ]]
        }

        assert cmd != null
        def lbTO =  cmd.lbs.find { it.lbUuid == lb2.uuid }
        assert lbTO != null
        def redirectRuleTO = lbTO.redirectRules.find { it.redirectRuleUuid == aclRule3.uuid }
        assert redirectRuleTO != null
        assert redirectRuleTO.redirectPort == 2000

        deleteAccessControlList {
            uuid = acl3.uuid
        }
    }

    void testUpgradeLoadBalancerForwardRule() {
        SQL.New(AccessControlListEntryVO.class)
            .eq(AccessControlListEntryVO_.type, AclEntryType.RedirectRule.toString())
            .set(AccessControlListEntryVO_.redirectPort, null)
            .update()
        def aclRule1 = Q.New(AccessControlListEntryVO.class)
                .eq(AccessControlListEntryVO_.name, "acl-rule-1")
                .find()
        assert aclRule1 != null
        assert aclRule1.getRedirectPort() == null
        def aclRule2 = Q.New(AccessControlListEntryVO.class)
                .eq(AccessControlListEntryVO_.name, "acl-rule-2")
                .find()
        assert aclRule2 != null
        assert aclRule2.getRedirectPort() == null

        lbMgr.upgradeLoadBalancerRedirectRule()       

        assert Q.New(AccessControlListEntryVO.class)
                .eq(AccessControlListEntryVO_.uuid, aclRule1.uuid)
                .eq(AccessControlListEntryVO_.redirectPort, 8080)
                .isExists()
        assert Q.New(AccessControlListEntryVO.class)
                .eq(AccessControlListEntryVO_.uuid, aclRule2.uuid)
                .eq(AccessControlListEntryVO_.redirectPort, 8080)
                .isExists()

        deleteAccessControlList {
            uuid = aclRule1.aclUuid
        }
        deleteAccessControlList {
            uuid = aclRule2.aclUuid
        }
    }

    @Override
    void clean() {
        def lb1 = env.inventoryByName("lb1") as LoadBalancerInventory
        deleteLoadBalancer {
            uuid = lb1.uuid
        }
        def lb2 = env.inventoryByName("lb2") as LoadBalancerInventory
        deleteLoadBalancer {
            uuid = lb2.uuid
        }
        env.delete()
    }
}
