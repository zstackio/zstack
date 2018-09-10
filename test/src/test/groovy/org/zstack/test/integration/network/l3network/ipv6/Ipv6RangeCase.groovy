package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L2NetworkInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.NetworkServiceProviderInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.network.IPv6Constants

import static java.util.Arrays.asList


/**
 * Created by shixin on 2018/09/10.
 */
class Ipv6RangeCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)

    }
    @Override
    void environment() {
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            testAttachIpRangeToL3Network()
            testAttachIpv6RangeOverLap()
            testAttachIpv6CidrOverLap()
            testAttachIpv6RangeAddressMode()
            testIpv6RangeWith2Ips()
        }
    }

    void testAttachIpRangeToL3Network(){
        L2NetworkInventory l2 = env.inventoryByName("l2")

        L3NetworkInventory l3_pub_ipv4 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "public-ipv4"
        }

        expect(AssertionError.class) {
            addIpv6RangeByNetworkCidr {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv4.getUuid()
                networkCidr = "2002:2001::/64"
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        L3NetworkInventory l3_pub_ipv6 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "public-ipv4"
            ipVersion = 6
        }

        expect(AssertionError.class) {
            addIpRange {
                name = "ipr-4"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "192.168.101.101"
                endIp = "192.168.101.200"
                gateway = "192.168.101.1"
                netmask = "255.255.255.0"
            }
        }

        expect(AssertionError.class) {
            addIpv6RangeByNetworkCidr {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                networkCidr = "2002:2001::/7"
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        expect(AssertionError.class) {
            addIpv6RangeByNetworkCidr {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                networkCidr = "2002:2001::/127"
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }
    }

    void testAttachIpv6CidrOverLap(){
        L2NetworkInventory l2 = env.inventoryByName("vlan-200")

        L3NetworkInventory l3_pub_ipv6 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "public-ipv6"
            ipVersion = 6
        }

        addIpv6RangeByNetworkCidr {
            name = "ipr-6"
            l3NetworkUuid = l3_pub_ipv6.getUuid()
            networkCidr = "2002:2001::/8"
            addressMode = IPv6Constants.Stateful_DHCP
        }

        expect(AssertionError.class) {
            addIpv6RangeByNetworkCidr {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                networkCidr = "2002:2001::/65"
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        expect(AssertionError.class) {
            addIpv6RangeByNetworkCidr {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                networkCidr = "2002:2001::/63"
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }
    }

    void testAttachIpv6RangeOverLap() {
        L2NetworkInventory l2 = env.inventoryByName("l2")

        L3NetworkInventory l3_pub_ipv6 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "public-ipv6"
            ipVersion = 6
        }

        addIpv6Range {
            name = "ipr-6"
            l3NetworkUuid = l3_pub_ipv6.getUuid()
            startIp = "2003:2001::0060"
            endIp = "2003:2001::00e0"
            gateway = "2003:2001::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        addIpv6Range {
            name = "ipr-6"
            l3NetworkUuid = l3_pub_ipv6.getUuid()
            startIp = "2003:2001::0160"
            endIp = "2003:2001::01e0"
            gateway = "2003:2001::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        L3NetworkInventory l3_private_ipv6 = createL3Network {
            category = "Private"
            l2NetworkUuid = l2.uuid
            name = "private-ipv6"
            ipVersion = 6
        }

        addIpv6Range {
            name = "ipr-6-single-address"
            l3NetworkUuid = l3_private_ipv6.getUuid()
            startIp = "2003:2002::2160"
            endIp = "2003:2002::2160"
            gateway = "2003:2002::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        expect(AssertionError.class) {
            addIpv6Range {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "2002:2001::0010"
                endIp = "2002:2001::0070"
                gateway = "2002:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        expect(AssertionError.class) {
            addIpv6Range {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "2002:2001::0030"
                endIp = "2002:2001::0080"
                gateway = "2002:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        expect(AssertionError.class) {
            addIpv6Range {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "2002:2001::0080"
                endIp = "2002:2001::0170"
                gateway = "2002:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        expect(AssertionError.class) {
            addIpv6Range {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "2002:2001::0170"
                endIp = "2002:2001::01a0"
                gateway = "2002:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        expect(AssertionError.class) {
            addIpv6Range {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "2002:2001::01a0"
                endIp = "2002:2001::01f0"
                gateway = "2002:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        expect(AssertionError.class) {
            addIpv6Range {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "2002:2001::0010"
                endIp = "2002:2001::00f0"
                gateway = "2002:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        expect(AssertionError.class) {
            addIpv6Range {
                name = "ipr-6"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "2002:2001::01a0"
                endIp = "2002:2001::03f0"
                gateway = "2002:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }
        }

        L3NetworkInventory l3_pri_ipv6 = createL3Network {
            category = "Private"
            l2NetworkUuid = l2.uuid
            name = "private-ipv6"
            ipVersion = 6
        }

        addIpv6Range {
            name = "ipr-6-2"
            l3NetworkUuid = l3_pri_ipv6.getUuid()
            startIp = "2203:2001::0001"
            endIp = "2203:2001::0002"
            gateway = "2203:2001::2"
            prefixLen = 126
            addressMode = IPv6Constants.Stateful_DHCP
        }
    }

    void testAttachIpv6RangeAddressMode() {
        L2NetworkInventory l2 = env.inventoryByName("l2")

        L3NetworkInventory l3_pub_ipv6 = createL3Network {
            category = "Public"
            l2NetworkUuid = l2.uuid
            name = "public-ipv6"
            ipVersion = 6
        }

        addIpv6Range {
            name = "ipr-7"
            l3NetworkUuid = l3_pub_ipv6.getUuid()
            startIp = "2004:2001::0060"
            endIp = "2004:2001::00e0"
            gateway = "2004:2001::2"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        expect(AssertionError.class) {
            addIpv6Range {
                name = "ipr-8"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "2004:2001::0160"
                endIp = "2004:2001::01e0"
                gateway = "2004:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateless_DHCP
            }
        }

        expect(AssertionError.class) {
            addIpv6Range {
                name = "ipr-8"
                l3NetworkUuid = l3_pub_ipv6.getUuid()
                startIp = "2004:2001::0160"
                endIp = "2004:2001::01e0"
                gateway = "2004:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.SLAAC
            }
        }
    }

    void testIpv6RangeWith2Ips() {
        L2NetworkInventory l2 = env.inventoryByName("l2")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        L3NetworkInventory l3_ipv6 = createL3Network {
            category = "Private"
            l2NetworkUuid = l2.uuid
            name = "2-ips"
            ipVersion = 6
        }

        def flatProvider = queryNetworkServiceProvider {
            delegate.conditions = ["type=Flat"]
        }[0] as NetworkServiceProviderInventory

        def netServices = ["${flatProvider.uuid}":["DHCP", "DNS", "Eip"]]
        attachNetworkServiceToL3Network {
            l3NetworkUuid = l3_ipv6.uuid
            networkServices = netServices
        }

        addIpv6Range {
            name = "ipr-2-ips"
            l3NetworkUuid = l3_ipv6.getUuid()
            startIp = "2004:2001::0002"
            endIp = "2004:2001::0003"
            gateway = "2004:2001::1"
            prefixLen = 64
            addressMode = IPv6Constants.Stateful_DHCP
        }

        VmInstanceInventory vm1 = createVmInstance {
            name = "vm-1"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_ipv6.uuid)
        }

        /* no free ip now */
        expect(AssertionError.class) {
            VmInstanceInventory vm2 = createVmInstance {
                name = "vm-2"
                instanceOfferingUuid = offering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = asList(l3_ipv6.uuid)
            }
        }
    }
}

