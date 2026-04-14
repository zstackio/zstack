package org.zstack.test.integration.kvm.vm.migrate

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.kvm.KVMHost
import org.zstack.sdk.HostInventory
import org.zstack.sdk.UpdateGlobalConfigAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/**
 * Verify that the libvirt TLS configuration (ZSTAC-81343) is correctly
 * propagated in the MigrateVmCmd sent to kvmagent.
 *
 * TLS certificate deployment is now handled by SSH-based detection +
 * ansible deploy (ZSTAC-83696), which skips in unit tests. Only
 * migration TLS flag propagation is tested here.
 *
 * Key logic under test (KVMHost.java):
 *   cmd.setUseTls(LIBVIRT_TLS_ENABLED && RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE)
 *   cmd.setSrcHostManagementIp(srcHostMnIp)
 */
class LibvirtTlsMigrateCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm1"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }
                    kvm {
                        name = "kvm2"
                        managementIp = "127.0.0.2"
                        username = "root"
                        password = "password"
                        usedMem = 1000
                        totalCpu = 10
                    }

                    attachPrimaryStorage("ps")
                    attachL2Network("l2")
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }

                cephPrimaryStorage {
                    name = "ps"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                    url = "ceph://pri"
                    fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                    monUrls = ["root:password@localhost/?monPort=7777"]
                }

                attachBackupStorage("bs")
            }

            cephBackupStorage {
                name = "bs"
                totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                url = "/bk"
                fsid = "7ff218d9-f525-435f-8a40-3618d1772a64"
                monUrls = ["root:password@localhost/?monPort=7777"]

                image {
                    name = "image"
                    url = "http://zstack.org/download/image.qcow2"
                }
            }

            vm {
                name = "vm"
                useCluster("cluster")
                useHost("kvm1")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
                useImage("image")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testSanIpParsing()
            testMigrateWithTlsEnabled()
            testMigrateWithTlsDisabled()
            testMigrateWithRestartLibvirtdDisabled()
            testGlobalConfigValidation()
        }
    }

    /**
     * Case 1: Both LIBVIRT_TLS_ENABLED=true and RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE=true
     *         => useTls should be true, srcHostManagementIp should be set
     */
    void testMigrateWithTlsEnabled() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def host1 = env.inventoryByName("kvm1") as HostInventory
        def host2 = env.inventoryByName("kvm2") as HostInventory

        // Ensure TLS is enabled (default is true)
        KVMGlobalConfig.LIBVIRT_TLS_ENABLED.updateValue("true")
        KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.updateValue("true")

        KVMAgentCommands.MigrateVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.MigrateVmCmd.class)
            return rsp
        }

        // Migrate vm from kvm1 to kvm2
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host2.uuid
        }

        assert cmd != null : "MigrateVmCmd should have been captured"
        assert cmd.useTls : "useTls should be true when both TLS and restartLibvirtd are enabled"
        assert cmd.srcHostManagementIp == host1.managementIp :
                "srcHostManagementIp should be source host management IP"
        assert cmd.destHostManagementIp == host2.managementIp :
                "destHostManagementIp should be dest host management IP"
    }

    /**
     * Case 2: LIBVIRT_TLS_ENABLED=false => useTls should be false regardless of restartLibvirtd
     */
    void testMigrateWithTlsDisabled() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def host1 = env.inventoryByName("kvm1") as HostInventory

        // Disable TLS
        KVMGlobalConfig.LIBVIRT_TLS_ENABLED.updateValue("false")
        KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.updateValue("true")

        KVMAgentCommands.MigrateVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.MigrateVmCmd.class)
            return rsp
        }

        // Migrate back to kvm1
        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host1.uuid
        }

        assert cmd != null : "MigrateVmCmd should have been captured"
        assert !cmd.useTls : "useTls should be false when TLS config is disabled"

        // Restore default
        KVMGlobalConfig.LIBVIRT_TLS_ENABLED.updateValue("true")
    }

    /**
     * Case 3: LIBVIRT_TLS_ENABLED=true but RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE=false
     *         => useTls should be false (AND logic: both must be true)
     *
     * This is a critical boundary: TLS config is on, but libvirtd was not restarted
     * with TLS certs deployed, so we must NOT tell kvmagent to use TLS.
     */
    void testMigrateWithRestartLibvirtdDisabled() {
        def vm = env.inventoryByName("vm") as VmInstanceInventory
        def host2 = env.inventoryByName("kvm2") as HostInventory

        KVMGlobalConfig.LIBVIRT_TLS_ENABLED.updateValue("true")
        KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.updateValue("false")

        KVMAgentCommands.MigrateVmCmd cmd = null
        env.afterSimulator(KVMConstant.KVM_MIGRATE_VM_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.MigrateVmCmd.class)
            return rsp
        }

        migrateVm {
            vmInstanceUuid = vm.uuid
            hostUuid = host2.uuid
        }

        assert cmd != null : "MigrateVmCmd should have been captured"
        assert !cmd.useTls :
                "useTls should be false when restartLibvirtd is disabled (TLS certs not deployed)"

        // Restore default
        KVMGlobalConfig.RECONNECT_HOST_RESTART_LIBVIRTD_SERVICE.updateValue("true")
    }

    /**
     * Case 4: Validate that libvirt.tls.enabled GlobalConfig only accepts true/false
     */
    void testGlobalConfigValidation() {
        // Valid values via SDK action
        updateGlobalConfig {
            category = "kvm"
            name = "libvirt.tls.enabled"
            value = "true"
        }
        assert KVMGlobalConfig.LIBVIRT_TLS_ENABLED.value(Boolean.class) == true

        updateGlobalConfig {
            category = "kvm"
            name = "libvirt.tls.enabled"
            value = "false"
        }
        assert KVMGlobalConfig.LIBVIRT_TLS_ENABLED.value(Boolean.class) == false

        // Invalid value should be rejected
        def action = new UpdateGlobalConfigAction()
        action.category = "kvm"
        action.name = "libvirt.tls.enabled"
        action.value = "invalid"
        action.sessionId = Test.currentEnvSpec.session.uuid
        UpdateGlobalConfigAction.Result res = action.call()
        assert res.error != null : "Setting an invalid value for libvirt.tls.enabled should fail"

        // Restore default
        updateGlobalConfig {
            category = "kvm"
            name = "libvirt.tls.enabled"
            value = "true"
        }
    }

    void testSanIpParsing() {
        // typical openssl SAN output
        def sanOutput = "            IP Address:10.0.0.10, IP Address:192.168.1.1, DNS:host.example.com\n"

        def ips = KVMHost.parseSanIps(sanOutput)
        assert ips.contains("10.0.0.10")
        assert ips.contains("192.168.1.1")
        assert ips.size() == 2 : "should only contain 2 IPs, got ${ips}"

        // prefix false-positive: 10.0.0.1 must NOT match when only 10.0.0.10 is in SAN
        assert !ips.contains("10.0.0.1") : "10.0.0.1 should not match 10.0.0.10"
        assert !ips.contains("192.168.1") : "partial IP should not match"

        // null / empty input
        assert KVMHost.parseSanIps(null).isEmpty()
        assert KVMHost.parseSanIps("").isEmpty()

        // multiline format
        def multiline = "X509v3 Subject Alternative Name:\n    IP Address:10.0.0.1\n    IP Address:10.0.0.10\n"
        def mlIps = KVMHost.parseSanIps(multiline)
        assert mlIps.contains("10.0.0.1")
        assert mlIps.contains("10.0.0.10")
        assert mlIps.size() == 2
        assert !mlIps.contains("10.0.0") : "prefix should not match"
    }
}
