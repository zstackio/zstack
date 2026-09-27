package org.zstack.test.integration.network.l3network

import org.zstack.core.db.Q
import org.zstack.header.network.l2.L2NetworkClusterRefVO
import org.zstack.header.network.l2.L2NetworkClusterRefVO_
import org.zstack.sdk.L2NetworkInventory
import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class UnattachedL2L3ConfigurationCase extends SubCase {
    EnvSpec env

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            zone {
                name = 'unattached-network-zone'
                l2NoVlanNetwork {
                    name = 'unattached-l2'
                    physicalInterface = 'eth0'
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            L2NetworkInventory l2 = env.inventoryByName('unattached-l2')
            assert !Q.New(L2NetworkClusterRefVO.class)
                    .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2.uuid).isExists()
            def l3 = createL3Network {
                name = 'l3-before-cluster-attachment'
                l2NetworkUuid = l2.uuid
            }
            def range = addIpRange {
                name = 'range-before-cluster-attachment'
                l3NetworkUuid = l3.uuid
                startIp = '192.0.2.10'
                endIp = '192.0.2.20'
                gateway = '192.0.2.1'
                netmask = '255.255.255.0'
            }
            assert range.l3NetworkUuid == l3.uuid
            assert range.startIp == '192.0.2.10'
            assert !Q.New(L2NetworkClusterRefVO.class)
                    .eq(L2NetworkClusterRefVO_.l2NetworkUuid, l2.uuid).isExists()
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}
