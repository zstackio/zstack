package org.zstack.test.integration.l3network.getfreeip

import org.zstack.sdk.FreeIpInventory
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.IpRangeSpec
import org.zstack.testlib.L3NetworkSpec
import org.zstack.testlib.Test
import org.zstack.utils.network.NetworkUtils

/**
 * Created by xing5 on 2017/2/21.
 */
class GetFreeIpTest extends Test {
    def DOC = """

    Test getting free IPs from a single L3 network with one or two IP ranges
    
"""
    EnvSpec env1
    EnvSpec env2
    EnvSpec env3

    @Override
    void setup() {
        spring {
            includeCoreServices()
            include("vip.xml")
        }
    }

    @Override
    void environment() {
        env1 = env {
            zone {
                name = "zone"

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            name = "ipr"
                            startIp = "10.223.110.10"
                            endIp = "10.223.110.20"
                            gateway = "10.223.110.1"
                            netmask = "255.255.255.0"
                        }
                    }
                }
            }
        }

        env2 = env1.copy()

        env3 = env {
            zone {
                name = "zone"

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        ip {
                            name = "ipr"
                            startIp = "10.223.110.10"
                            endIp = "10.223.110.20"
                            gateway = "10.223.110.1"
                            netmask = "255.255.255.0"
                        }

                        ip {
                            name = "ipr2"
                            startIp = "10.223.110.50"
                            endIp = "10.223.110.60"
                            gateway = "10.223.110.1"
                            netmask = "255.255.255.0"
                        }
                    }
                }
            }
        }
    }



    void occupyFourIPs() {
        L3NetworkSpec l3 = env2.specByName("l3")

        for (int i=0; i<4; i++) {
            createVip {
                name = "non-use-vip"
                l3NetworkUuid = l3.inventory.uuid
            }
        }
    }

    void useL3NetworkUuidGetFreeIPsAfterOccupyFourIPs() {
        L3NetworkSpec l3 = specByName("l3")
        IpRangeSpec ipr = specByName("ipr")

        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr.inventory.startIp, ipr.inventory.endIp) - 4
    }

    void useIpRangeUuidGetFreeIPsAfterOccupyFourIPs() {
        IpRangeSpec ipr = specByName("ipr")

        List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr.inventory.startIp, ipr.inventory.endIp) - 4
    }

    void useL3NetworkUuidTestTwoIpRangesSize() {
        L3NetworkSpec l3 = specByName("l3")
        IpRangeSpec ipr1 = specByName("ipr")
        IpRangeSpec ipr2 = specByName("ipr2")

        List<FreeIpInventory> freeIps = getFreeIpOfL3Network {
            l3NetworkUuid = l3.inventory.uuid
        }

        assert freeIps.size() ==  NetworkUtils.ipRangeLength(ipr1.startIp, ipr1.endIp) + NetworkUtils.ipRangeLength(ipr2.startIp, ipr2.endIp)
    }

    void useTwoIpRangesUuidTestTwoIpRangesSize() {
        IpRangeSpec ipr1 = specByName("ipr")
        List<FreeIpInventory> freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr1.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr1.startIp, ipr1.endIp)

        IpRangeSpec ipr2 = specByName("ipr2")
        freeIps = getFreeIpOfIpRange {
            ipRangeUuid = ipr2.inventory.uuid
        }

        assert freeIps.size() == NetworkUtils.ipRangeLength(ipr2.startIp, ipr2.endIp)
    }

    void testTwoIpRangesInSingleL3Network() {
        env3.create {
            useL3NetworkUuidTestTwoIpRangesSize()
            useTwoIpRangesUuidTestTwoIpRangesSize()
            useTwoIpRangesUuidTestTwoIpRangesSize()
        }.delete()
    }

    @Override
    void test() {


        env2.create {
            occupyFourIPs()
            useL3NetworkUuidGetFreeIPsAfterOccupyFourIPs()
            useIpRangeUuidGetFreeIPsAfterOccupyFourIPs()
        }.delete()

        testTwoIpRangesInSingleL3Network()
    }

}
