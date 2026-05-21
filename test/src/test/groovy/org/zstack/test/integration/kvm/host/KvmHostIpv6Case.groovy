package org.zstack.test.integration.kvm.host

import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class KvmHostIpv6Case extends SubCase {
    EnvSpec env
    ClusterInventory cluster

    private static final String LOOPBACK_IPV6_FULL = "0:0:0:0:0:0:0:1"
    private static final String LOOPBACK_IPV6_CANONICAL = "::1"
    private static final String LINK_LOCAL_IPV6 = "fe80::1"
    private static final String INVALID_MANAGEMENT_IP = "not-an-ip!!"

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            cluster = env.inventoryByName("cluster") as ClusterInventory
            testAddHostWithIpv6()
            testRejectInvalidAndLinkLocalIpv6()
        }
    }

    void testAddHostWithIpv6() {
        def action = new AddKVMHostAction()
        action.sessionId = adminSession()
        action.resourceUuid = Platform.uuid
        action.clusterUuid = cluster.uuid
        action.managementIp = LOOPBACK_IPV6_FULL
        action.name = "kvm-ipv6"
        action.username = "root"
        action.password = "password"
        def res = action.call()

        assert res.error == null
        assert (res.value.inventory as KVMHostInventory).managementIp == LOOPBACK_IPV6_CANONICAL
        assert Q.New(HostVO.class).eq(HostVO_.managementIp, LOOPBACK_IPV6_CANONICAL).isExists()
    }

    void testRejectInvalidAndLinkLocalIpv6() {
        long before = Q.New(HostVO.class).count()

        def action = new AddKVMHostAction()
        action.sessionId = adminSession()
        action.resourceUuid = Platform.uuid
        action.clusterUuid = cluster.uuid
        action.managementIp = INVALID_MANAGEMENT_IP
        action.name = "kvm-invalid"
        action.username = "root"
        action.password = "password"
        def res = action.call()

        assert res.error != null
        assert res.error.code == SysErrors.INVALID_ARGUMENT_ERROR.toString()
        assert Q.New(HostVO.class).count() == before

        action = new AddKVMHostAction()
        action.sessionId = adminSession()
        action.resourceUuid = Platform.uuid
        action.clusterUuid = cluster.uuid
        action.managementIp = LINK_LOCAL_IPV6
        action.name = "kvm-link-local"
        action.username = "root"
        action.password = "password"
        res = action.call()

        assert res.error != null
        assert res.error.code == SysErrors.INVALID_ARGUMENT_ERROR.toString()
        assert Q.New(HostVO.class).count() == before
    }
}
