package org.zstack.test.integration.network.l3network.getfreeip

import org.zstack.sdk.FreeIpInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.IpRangeSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.sdk.*
import org.zstack.testlib.SubCase
import org.zstack.utils.network.NetworkUtils
import org.zstack.utils.network.IPv6Constants


class CreateVipWithIpVersionCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
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
                        name = "l3"
                    }
                }
            }
        }
    }


    @Override
    void test() {
        env.create {

            def l3 = env.inventoryByName("l3") as L3NetworkInventory

            IpRangeInventory ipr4 = addIpRange {
                name = "ipr-4"
                l3NetworkUuid = l3.uuid
                startIp = "10.223.110.10"
                endIp = "10.223.110.20"
                gateway = "10.223.110.1"
                netmask = "255.255.255.0"
            }

            IpRangeInventory ipr6 = addIpv6Range {
                name = "ipr-6"
                l3NetworkUuid = l3.uuid
                startIp = "2003:2001::0010"
                endIp = "2003:2001::0020"
                gateway = "2003:2001::2"
                prefixLen = 64
                addressMode = IPv6Constants.Stateful_DHCP
            }

            VipInventory vip_1 = createVip {
                name = "vip"
                l3NetworkUuid = l3.uuid
            }
            UsedIpInventory ip_1 = queryIpAddress{conditions=["ip="+vip_1.getIp()]}[0]
            assert  ip_1.getIpVersion() == IPv6Constants.IPv4

            VipInventory vip_2 = createVip {
                name = "vip"
                l3NetworkUuid = l3.uuid
                ipVersion = IPv6Constants.IPv4
            }
            UsedIpInventory ip_2 = queryIpAddress{conditions=["ip="+vip_2.getIp()]}[0]
            assert  ip_2.getIpVersion() == IPv6Constants.IPv4

            VipInventory vip_3 = createVip {
                name = "vip"
                l3NetworkUuid = l3.uuid
                ipVersion = IPv6Constants.IPv6
            }
            UsedIpInventory ip_3 = queryIpAddress{conditions=["ip="+vip_3.getIp()]}[0]
            assert  ip_3.getIpVersion() == IPv6Constants.IPv6
        }
    }


    @Override
    void clean() {
        env.delete()
    }
}
