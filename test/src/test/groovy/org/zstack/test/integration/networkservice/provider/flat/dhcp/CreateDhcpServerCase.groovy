package org.zstack.test.integration.networkservice.provider.flat.dhcp

import org.zstack.core.db.Q
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.network.IPv6Constants

import static org.zstack.network.service.flat.FlatNetworkSystemTags.L3_NETWORK_DHCP_IP
import static org.zstack.network.service.flat.FlatNetworkSystemTags.L3_NETWORK_DHCP6_IP
import static org.zstack.network.service.flat.FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_NETWORK_SERVICE_FLAT_10037

/**
 * Created by shixin on 2018/12/19.
 */
class CreateDhcpServerCase extends SubCase {

    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
        spring {
            ceph()
            flatNetwork()
        }
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
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
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
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                    }

                    l3Network {
                        name = "l3-ipv6"
                        ipVersion = 6

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testAddDhcpServerIp()
            testAddDhcpv6ServerIp()
            testDualStackDhcpTags()
        }
    }

    void testAddDhcpServerIp() {
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        String dhcpIp = "192.168.0.105"
        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.3"
                endIp = "192.168.0.254"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.2")]
            }  
        }

        IpRangeInventory ipr1 = addIpRange {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            startIp = "192.168.0.3"
            endIp = "192.168.0.30"
            netmask = "255.255.255.0"
            gateway = "192.168.0.1"
            systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", dhcpIp)]
        }

        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.103"
                endIp = "192.168.0.130"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.2")]
            }
        }

        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.103"
                endIp = "192.168.0.130"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.110")]
            }
        }
        String dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == dhcpIp

        IpRangeInventory ipr2 = addIpRange {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            startIp = "192.168.0.103"
            endIp = "192.168.0.130"
            netmask = "255.255.255.0"
            gateway = "192.168.0.1"
        }
        assert Q.New(UsedIpVO.class).eq(UsedIpVO_.ip, dhcpIp).count() == 1

        IpRangeInventory ipr3 = addIpRange {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            startIp = "192.168.0.203"
            endIp = "192.168.0.230"
            netmask = "255.255.255.0"
            gateway = "192.168.0.1"
        }

        deleteIpRange {
            uuid = ipr1.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == dhcpIp

        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.60"
                endIp = "192.168.0.70"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.2")]
            }
        }

        deleteIpRange {
            uuid = ipr2.uuid
        }

        deleteIpRange {
            uuid = ipr3.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == null

        IpRangeInventory ipr4 = addIpRangeByNetworkCidr {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            networkCidr = "192.168.0.0/24"
            systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.0.4")]
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == "192.168.0.4"

        deleteIpRange {
            uuid = ipr4.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == null

        IpRangeInventory ipr5 = addIpRange {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            startIp = "192.168.0.60"
            endIp = "192.168.0.70"
            netmask = "255.255.255.0"
            gateway = "192.168.0.1"
        }
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp != null

        expect (AssertionError.class) {
            addIpRange {
                l3NetworkUuid = l3.uuid
                name = "test-ip-range"
                startIp = "192.168.0.103"
                endIp = "192.168.0.130"
                netmask = "255.255.255.0"
                gateway = "192.168.0.1"
                systemTags = [String.format("flatNetwork::DhcpServer::%s::ipUuid::null", "192.168.1.110")]
            }
        }

        testZSTAC86900DeleteReservedIp(l3, IPv6Constants.IPv4)
    }

    void testAddDhcpv6ServerIp() {
        L3NetworkInventory l3 = env.inventoryByName("l3-ipv6")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::10"
                endIp = "1000::20"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::Dhcp6Server::%s::ipUuid::null", "2000--2")]
            }
        }

        IpRangeInventory ipr1 = addIpv6Range {
            l3NetworkUuid = l3.uuid
            name = "test-ipv6-range"
            startIp = "1000::10"
            endIp = "1000::20"
            prefixLen = 64
            gateway = "1000::1"
            addressMode = IPv6Constants.Stateful_DHCP
            systemTags = [String.format("flatNetwork::Dhcp6Server::%s::ipUuid::null", "1000--2")]
        }

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::30"
                endIp = "1000::40"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::Dhcp6Server::%s::ipUuid::null", "1000--2")]
            }
        }

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::50"
                endIp = "1000::60"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::Dhcp6Server::%s::ipUuid::null", "1000--12")]
            }
        }

        IpRangeInventory ipr2 = addIpv6Range {
            l3NetworkUuid = l3.uuid
            name = "test-ipv6-range-2"
            startIp = "1000::30"
            endIp = "1000::40"
            prefixLen = 64
            gateway = "1000::1"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        IpRangeInventory ipr3 = addIpv6Range {
            l3NetworkUuid = l3.uuid
            name = "test-ipv6-range-2"
            startIp = "1000::60"
            endIp = "1000::70"
            prefixLen = 64
            gateway = "1000::1"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        deleteIpRange {
            uuid = ipr1.uuid
        }

        String dhcpServerIp = L3_NETWORK_DHCP6_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == "1000--2"

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::150"
                endIp = "1000::160"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::Dhcp6Server::%s::ipUuid::null", "1000--155")]
            }
        }

        deleteIpRange {
            uuid = ipr2.uuid
        }

        deleteIpRange {
            uuid = ipr3.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP6_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == null

        IpRangeInventory ipr4 = addIpv6RangeByNetworkCidr {
            l3NetworkUuid = l3.uuid
            name = "test-ip-range"
            networkCidr = "1000::/64"
            addressMode = IPv6Constants.Stateful_DHCP
            systemTags = [String.format("flatNetwork::Dhcp6Server::%s::ipUuid::null", "1000--155")]
        }

        dhcpServerIp = L3_NETWORK_DHCP6_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == "1000--155"

        deleteIpRange {
            uuid = ipr4.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP6_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp == null

        IpRangeInventory ipr5 = addIpv6Range {
            l3NetworkUuid = l3.uuid
            name = "test-ipv6-range"
            startIp = "1000::150"
            endIp = "1000::160"
            prefixLen = 64
            gateway = "1000::1"
            addressMode = IPv6Constants.Stateful_DHCP
        }
        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
        }

        dhcpServerIp = L3_NETWORK_DHCP6_IP.getTokenByResourceUuid(ipr1.getL3NetworkUuid(), L3_NETWORK_DHCP_IP_TOKEN)
        assert dhcpServerIp != null

        expect (AssertionError.class) {
            addIpv6Range {
                l3NetworkUuid = l3.uuid
                name = "test-ipv6-range"
                startIp = "1000::250"
                endIp = "1000::260"
                prefixLen = 64
                gateway = "1000::1"
                addressMode = IPv6Constants.Stateful_DHCP
                systemTags = [String.format("flatNetwork::Dhcp6Server::%s::ipUuid::null", "1000--12")]
            }
        }

        testZSTAC86900DeleteReservedIp(l3, IPv6Constants.IPv6)
    }

    void testZSTAC86900DeleteReservedIp(L3NetworkInventory l3, int requestedIpVersion) {
        String reservedIp = getFreeIp {
            l3NetworkUuid = l3.uuid
            ipVersion = requestedIpVersion
            limit = 1
        }[0].ip
        ReservedIpRangeInventory reservedIpRange = addReservedIpRange {
            l3NetworkUuid = l3.uuid
            startIp = reservedIp
            endIp = reservedIp
        }
        UsedIpInventory reservedUsedIp = queryIpAddress {
            conditions = ["l3NetworkUuid=${l3.uuid}", "ip=${reservedIp}"]
        }[0]

        GetL3NetworkDhcpIpAddressResult dhcpAddressResult = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3.uuid
        }
        String dhcpServerIp = dhcpAddressResult.ip6 ?: dhcpAddressResult.ip
        UsedIpInventory dhcpUsedIp = queryIpAddress {
            conditions = ["l3NetworkUuid=${l3.uuid}", "ip=${dhcpServerIp}"]
        }[0]

        DeleteIpAddressAction batchDelete = new DeleteIpAddressAction(
                l3NetworkUuid: l3.uuid,
                usedIpUuids: [reservedUsedIp.uuid, dhcpUsedIp.uuid],
                sessionId: adminSession()
        )
        DeleteIpAddressAction.Result batchResult = batchDelete.call()
        assert batchResult.error.globalErrorCode == ORG_ZSTACK_NETWORK_SERVICE_FLAT_10037
        assert Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, reservedUsedIp.uuid).isExists()
        assert Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, dhcpUsedIp.uuid).isExists()

        deleteIpAddress {
            l3NetworkUuid = l3.uuid
            usedIpUuids = [reservedUsedIp.uuid]
        }
        assert !Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, reservedUsedIp.uuid).isExists()

        DeleteIpAddressAction dhcpDelete = new DeleteIpAddressAction(
                l3NetworkUuid: l3.uuid,
                usedIpUuids: [dhcpUsedIp.uuid],
                sessionId: adminSession()
        )
        DeleteIpAddressAction.Result dhcpResult = dhcpDelete.call()
        assert dhcpResult.error.globalErrorCode == ORG_ZSTACK_NETWORK_SERVICE_FLAT_10037
        assert Q.New(UsedIpVO.class).eq(UsedIpVO_.uuid, dhcpUsedIp.uuid).isExists()

        deleteReservedIpRange {
            uuid = reservedIpRange.uuid
        }
    }

    void testDualStackDhcpTags() {
        def l3 = createL3Network {
            name = 'dual-stack-dhcp-tags'
            l2NetworkUuid = env.inventoryByName('l2').uuid
            ipVersion = IPv6Constants.IPv4
        }
        def v4 = addIpRange {
            name = 'dual-v4'; l3NetworkUuid = l3.uuid
            startIp = '192.0.2.10'; endIp = '192.0.2.30'
            gateway = '192.0.2.1'; netmask = '255.255.255.0'
        }
        def v6 = addIpv6Range {
            name = 'dual-v6'; l3NetworkUuid = l3.uuid
            startIp = '2001:db8::10'; endIp = '2001:db8::30'
            gateway = '2001:db8::1'; prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = ['Flat': ['DHCP']]
            systemTags = ['flatNetwork::DhcpServer::192.0.2.11::ipUuid::null',
                          'flatNetwork::Dhcp6Server::2001:db8--11::ipUuid::null']
        }
        def v4Tags = L3_NETWORK_DHCP_IP.getTokensOfTagsByResourceUuid(l3.uuid)
        def v6Tags = L3_NETWORK_DHCP6_IP.getTokensOfTagsByResourceUuid(l3.uuid)
        assert v4Tags.size() == 1 && v6Tags.size() == 1
        assert v4Tags[0]['ip'] == '192.0.2.11'
        assert v6Tags[0]['ip'] == '2001:db8--11'
        assert Q.New(UsedIpVO).eq(UsedIpVO_.l3NetworkUuid, l3.uuid).count() == 2
        changeL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3.uuid
            dhcpv6ServerIp = '2001:db8::12'
        }
        assert L3_NETWORK_DHCP_IP.getTokensOfTagsByResourceUuid(l3.uuid) == v4Tags
        assert L3_NETWORK_DHCP6_IP.getTags(l3.uuid).size() == 1
        assert L3_NETWORK_DHCP6_IP.getTokenByResourceUuid(l3.uuid, L3_NETWORK_DHCP_IP_TOKEN) == '2001:db8--12'
        assert !Q.New(UsedIpVO).eq(UsedIpVO_.uuid, v6Tags[0]['ipUuid']).isExists()
        deleteIpRange { uuid = v6.uuid }
        assert !L3_NETWORK_DHCP6_IP.hasTag(l3.uuid)
        assert L3_NETWORK_DHCP_IP.getTokensOfTagsByResourceUuid(l3.uuid) == v4Tags
        assert Q.New(UsedIpVO).eq(UsedIpVO_.l3NetworkUuid, l3.uuid).count() == 1
        detachNetworkServiceFromL3Network {
            l3NetworkUuid = l3.uuid
            networkServices = ['Flat': ['DHCP']]
        }
        assert !L3_NETWORK_DHCP_IP.hasTag(l3.uuid)
        assert !L3_NETWORK_DHCP6_IP.hasTag(l3.uuid)
        assert Q.New(UsedIpVO).eq(UsedIpVO_.l3NetworkUuid, l3.uuid).count() == 0
        deleteL3Network { uuid = l3.uuid }
    }

    @Override
    void clean() {
        env.delete()
    }

}
