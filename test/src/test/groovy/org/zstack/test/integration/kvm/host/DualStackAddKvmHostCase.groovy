package org.zstack.test.integration.kvm.host

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.header.host.ConnectHostMsg
import org.zstack.header.host.ConnectHostReply
import org.zstack.header.host.CpuArchitecture
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.header.tag.SystemTagVO
import org.zstack.header.tag.SystemTagVO_
import org.zstack.header.zone.ZoneVO
import org.zstack.kvm.KVMHost
import org.zstack.kvm.KVMHostVO
import org.zstack.sdk.AddKVMHostAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.lang.reflect.Field

class DualStackAddKvmHostCase extends SubCase {
    private static final String MN_IPV4 = "192.168.1.10"
    private static final String MN_IPV6 = "2001:db8::1"
    private static final String HOST_IPV4 = "192.168.1.20"
    private static final String HOST_IPV6 = "2001:db8::10"
    private static final int REST_PORT = 8080

    private EnvSpec env
    private ClusterInventory cluster
    private DatabaseFacade dbf

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
            prepareConnectHostReply()

            updateZoneIpVersion(cluster.zoneUuid, "ipv6")
            KVMHostInventory ipv6Host
            withManagementServerIpProperties([
                    "management.server.ip" : MN_IPV4,
                    "management.server.ip6": MN_IPV6,
            ]) {
                ipv6Host = addIpv6KvmHost()
                assertIpv6CallbackPrecheckCommand()
            }

            deleteHost {
                uuid = ipv6Host.uuid
            }
            updateZoneIpVersion(cluster.zoneUuid, "ipv4")
            withManagementServerIpProperties([
                    "management.server.ip" : MN_IPV6,
                    "management.server.ip4": MN_IPV4,
            ]) {
                addIpv4KvmHost()
                assertIpv4CallbackPrecheckCommand()
            }
        }
    }

    private void updateZoneIpVersion(String zoneUuid, String ipVersion) {
        SystemTagVO currentTag = Q.New(SystemTagVO.class)
                .eq(SystemTagVO_.resourceUuid, zoneUuid)
                .eq(SystemTagVO_.resourceType, ZoneVO.simpleName)
                .like(SystemTagVO_.tag, "managementNetwork::ipVersion::%")
                .find()
        assert currentTag != null

        updateSystemTag {
            uuid = currentTag.uuid
            tag = "managementNetwork::ipVersion::${ipVersion}"
        }
    }

    private void prepareConnectHostReply() {
        env.message(ConnectHostMsg.class) { ConnectHostMsg msg, CloudBus bus ->
            KVMHostVO host = dbf.findByUuid(msg.uuid, KVMHostVO.class)
            host.setArchitecture(CpuArchitecture.x86_64.name())
            host.setOsDistribution("centos")
            host.setOsRelease("core")
            host.setOsVersion("7.6.1810")
            dbf.update(host)
            bus.reply(msg, new ConnectHostReply())
        }
    }

    private KVMHostInventory addIpv6KvmHost() {
        return addKvmHost(HOST_IPV6, "dual-stack-kvm-ipv6")
    }

    private KVMHostInventory addIpv4KvmHost() {
        return addKvmHost(HOST_IPV4, "dual-stack-kvm-ipv4")
    }

    private KVMHostInventory addKvmHost(String managementIp, String name) {
        AddKVMHostAction action = new AddKVMHostAction()
        action.sessionId = adminSession()
        action.resourceUuid = Platform.uuid
        action.clusterUuid = cluster.uuid
        action.managementIp = managementIp
        action.name = name
        action.username = "root"
        action.password = "password"

        def result = action.call()

        assert result.error == null
        KVMHostInventory inventory = result.value.inventory as KVMHostInventory
        assert inventory.managementIp == managementIp
        assert Q.New(HostVO.class).eq(HostVO_.managementIp, managementIp).isExists()
        return inventory
    }

    private void assertIpv6CallbackPrecheckCommand() {
        RESTFacade restf = [
                buildCallbackUrl: { String host -> RESTFacadeImpl.buildCallbackUrl(host, REST_PORT, "zstack") }
        ] as RESTFacade

        def command = KVMHost.buildManagementNodeCallbackCheckCommand(HOST_IPV6, restf)
        String callbackUrl = RESTFacadeImpl.buildCallbackUrl(MN_IPV6, REST_PORT, "zstack")

        assert command.success
        assert command.result.contains("curl --connect-timeout 10 --max-time 15 ${callbackUrl}")
        assert command.result.contains("wget --spider -q --connect-timeout=10 --read-timeout=10 --tries=1 ${callbackUrl}")
        assert !command.result.contains("http://${MN_IPV4}:${REST_PORT}/zstack${RESTConstant.CALLBACK_PATH}")
    }

    private void assertIpv4CallbackPrecheckCommand() {
        RESTFacade restf = [
                buildCallbackUrl: { String host -> RESTFacadeImpl.buildCallbackUrl(host, REST_PORT, "zstack") }
        ] as RESTFacade

        def command = KVMHost.buildManagementNodeCallbackCheckCommand(HOST_IPV4, restf)
        String callbackUrl = RESTFacadeImpl.buildCallbackUrl(MN_IPV4, REST_PORT, "zstack")

        assert command.success
        assert command.result.contains("curl --connect-timeout 10 --max-time 15 ${callbackUrl}")
        assert command.result.contains("wget --spider -q --connect-timeout=10 --read-timeout=10 --tries=1 ${callbackUrl}")
        assert !command.result.contains("http://[${MN_IPV6}]:${REST_PORT}/zstack${RESTConstant.CALLBACK_PATH}")
    }

    private void withManagementServerIpProperties(Map<String, String> properties, Closure closure) {
        List<String> managedKeys = [
                "management.server.ip",
                "management.server.ip4",
                "management.server.ip6",
        ]
        Map<String, String> oldValues = [:]
        managedKeys.each { key -> oldValues[key] = System.getProperty(key) }

        try {
            resetCachedManagementServerIp()
            managedKeys.each { key -> System.clearProperty(key) }
            properties.each { key, value -> System.setProperty(key, value) }
            closure.call()
        } finally {
            managedKeys.each { key ->
                if (oldValues[key] == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, oldValues[key])
                }
            }
            resetCachedManagementServerIp()
        }
    }

    private void resetCachedManagementServerIp() {
        Field field = Platform.class.getDeclaredField("managementServerIp")
        field.setAccessible(true)
        field.set(null, null)
    }
}
