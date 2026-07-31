package org.zstack.test.integration.networkservice.provider.virtualrouter.loadbalancer

import org.zstack.appliancevm.ApplianceVmConstant
import org.zstack.appliancevm.ApplianceVmHaStatus
import org.zstack.appliancevm.ApplianceVmStatus
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.identity.AccountConstant
import org.zstack.header.image.ImagePlatform
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.lb.LoadBalancerListenerVO
import org.zstack.network.service.lb.LoadBalancerListenerVO_
import org.zstack.network.service.lb.LoadBalancerVO
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.VirtualRouterMetadataVO
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO
import org.zstack.network.service.virtualrouter.lb.VirtualRouterLoadBalancerRefVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.CreateLoadBalancerListenerAction
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.LoadBalancerInventory
import org.zstack.sdk.VipInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class TcpIpvsZvrVersionValidationCase extends SubCase {
    EnvSpec env
    DatabaseFacade dbf
    String syntheticVrUuid

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = "zone"

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
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING]
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
            }
        }
    }

    @Override
    void test() {
        env.create {
            dbf = bean(DatabaseFacade.class)
            testTcpIpvsCreateRejectedByOldZvrVersion()
        }
    }

    @Override
    void clean() {
        if (syntheticVrUuid != null) {
            VirtualRouterLoadBalancerRefVO ref = Q.New(VirtualRouterLoadBalancerRefVO.class)
                    .eq(VirtualRouterLoadBalancerRefVO_.virtualRouterVmUuid, syntheticVrUuid)
                    .find()
            if (ref != null) {
                dbf.remove(ref)
            }

            VirtualRouterMetadataVO metadata = dbf.findByUuid(syntheticVrUuid, VirtualRouterMetadataVO.class)
            if (metadata != null) {
                dbf.remove(metadata)
            }

            VirtualRouterVmVO vr = dbf.findByUuid(syntheticVrUuid, VirtualRouterVmVO.class)
            if (vr != null) {
                dbf.remove(vr)
            }
        }
        env.delete()
    }

    void testTcpIpvsCreateRejectedByOldZvrVersion() {
        L3NetworkInventory publicL3 = env.inventoryByName("publicL3") as L3NetworkInventory

        VipInventory vip = createVip {
            name = "tcp-ipvs-zvr-version-vip"
            l3NetworkUuid = publicL3.uuid
        }

        LoadBalancerInventory lb = createLoadBalancer {
            name = "tcp-ipvs-zvr-version-lb"
            vipUuid = vip.uuid
        }
        setLbProviderType(lb.uuid)

        String vrUuid = createVirtualRouterRef(lb.uuid)
        setZvrVersion(vrUuid, "5.5.28.0")

        CreateLoadBalancerListenerAction action = createTcpIpvsListenerAction(lb.uuid, "tcp-ipvs-old-zvr-version", 19095)
        CreateLoadBalancerListenerAction.Result result = action.call()

        assert result.error != null
        assert result.error.globalErrorCode == "ORG_ZSTACK_NETWORK_SERVICE_LB_10190"
        assert result.error.details.contains("does not support tcp ipvs listener")
        assert !Q.New(LoadBalancerListenerVO.class)
                .eq(LoadBalancerListenerVO_.name, action.name)
                .isExists()

        setZvrVersion(vrUuid, VyosConstants.TCP_IPVS_MIN_ZVR_VERSION)
        result = createTcpIpvsListenerAction(lb.uuid, "tcp-ipvs-supported-zvr-version", 19096).call()
        assert result.error == null
    }

    String createVirtualRouterRef(String lbUuid) {
        String vrUuid = Platform.getUuid()
        VirtualRouterVmVO vr = new VirtualRouterVmVO()
        vr.uuid = vrUuid
        vr.name = "tcp-ipvs-zvr-version-vr"
        vr.type = ApplianceVmConstant.APPLIANCE_VM_TYPE
        vr.applianceVmType = VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE
        vr.hypervisorType = "KVM"
        vr.platform = ImagePlatform.Linux.toString()
        vr.state = VmInstanceState.Running
        vr.status = ApplianceVmStatus.Connected
        vr.haStatus = ApplianceVmHaStatus.NoHa
        vr.agentPort = 7272
        vr.internalId = 1L
        vr.accountUuid = AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
        dbf.persist(vr)
        syntheticVrUuid = vrUuid

        VirtualRouterLoadBalancerRefVO ref = new VirtualRouterLoadBalancerRefVO()
        ref.loadBalancerUuid = lbUuid
        ref.virtualRouterVmUuid = vrUuid
        dbf.persist(ref)

        return vrUuid
    }

    void setLbProviderType(String lbUuid) {
        LoadBalancerVO lbVO = dbf.findByUuid(lbUuid, LoadBalancerVO.class)
        lbVO.providerType = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
        dbf.update(lbVO)
    }

    void setZvrVersion(String vrUuid, String version) {
        VirtualRouterMetadataVO metadata = dbf.findByUuid(vrUuid, VirtualRouterMetadataVO.class)
        if (metadata == null) {
            metadata = new VirtualRouterMetadataVO()
            metadata.uuid = vrUuid
            metadata.zvrVersion = version
            dbf.persist(metadata)
        } else {
            metadata.zvrVersion = version
            dbf.update(metadata)
        }
    }

    CreateLoadBalancerListenerAction createTcpIpvsListenerAction(String lbUuid, String name, int port) {
        CreateLoadBalancerListenerAction action = new CreateLoadBalancerListenerAction()
        action.name = name
        action.loadBalancerUuid = lbUuid
        action.protocol = LoadBalancerConstants.LB_PROTOCOL_TCP
        action.loadBalancerPort = port
        action.instancePort = 8080
        action.dataPlane = LoadBalancerConstants.DATA_PLANE_IPVS
        action.forwardMode = LoadBalancerConstants.FORWARD_MODE_FULL_NAT
        action.sessionId = adminSession()
        return action
    }
}
