package org.zstack.test.integration.kvm.host

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.ConnectHostReply
import org.zstack.header.host.CpuArchitecture
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.kvm.KVMHostVO
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class KvmHostIpv6Case extends SubCase {
    EnvSpec env
    ClusterInventory cluster
    DatabaseFacade dbf

    private static final String GLOBAL_IPV6_FULL = "2001:0db8:0000:0000:0000:0000:0000:0010"
    private static final String GLOBAL_IPV6_CANONICAL = "2001:db8::10"
    private static final String LINK_LOCAL_IPV6 = "fe80::1"
    private static final String LOOPBACK_IPV6 = "::1"
    private static final String INVALID_MANAGEMENT_IP = "not-an-ip!!"
    private static final String OS_DISTRIBUTION = "centos"
    private static final String OS_RELEASE = "core"
    private static final String OS_VERSION = "7.6.1810"

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
            dbf = bean(DatabaseFacade.class)
            cluster = env.inventoryByName("cluster") as ClusterInventory
            testAddHostWithIpv6()
            testRejectInvalidAndLinkLocalIpv6()
        }
    }

    void testAddHostWithIpv6() {
        env.message(ConnectHostMsg.class) { ConnectHostMsg msg, CloudBus bus ->
            KVMHostVO host = dbf.findByUuid(msg.uuid, KVMHostVO.class)
            host.setArchitecture(CpuArchitecture.x86_64.name())
            host.setOsDistribution(OS_DISTRIBUTION)
            host.setOsRelease(OS_RELEASE)
            host.setOsVersion(OS_VERSION)
            dbf.update(host)
            bus.reply(msg, new ConnectHostReply())
        }

        def action = new AddKVMHostAction()
        action.sessionId = adminSession()
        action.resourceUuid = Platform.uuid
        action.clusterUuid = cluster.uuid
        action.managementIp = GLOBAL_IPV6_FULL
        action.name = "kvm-ipv6"
        action.username = "root"
        action.password = "password"
        def res = action.call()

        assert res.error == null
        assert (res.value.inventory as KVMHostInventory).managementIp == GLOBAL_IPV6_CANONICAL
        assert Q.New(HostVO.class).eq(HostVO_.managementIp, GLOBAL_IPV6_CANONICAL).isExists()
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
        action.managementIp = LOOPBACK_IPV6
        action.name = "kvm-loopback"
        action.username = "root"
        action.password = "password"
        res = action.call()

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
