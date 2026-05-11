package org.zstack.test.integration.kvm

import org.springframework.http.HttpEntity
import org.zstack.compute.allocator.HostAllocatorGlobalConfig
import org.zstack.compute.allocator.PhysicalServerCapacityUpdater
import org.zstack.compute.host.HostGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.server.PhysicalServerCapacityVO
import org.zstack.header.server.PhysicalServerCapacityVO_
import org.zstack.header.server.PhysicalServerRoleVO
import org.zstack.header.server.PhysicalServerRoleVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.PhysicalServerInventory
import org.zstack.sdk.ServerPoolInventory
import org.zstack.sdk.ZoneInventory
import org.zstack.test.integration.kvm.host.HostEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static org.zstack.kvm.KVMConstant.KVM_HOST_FACT_PATH

/**
 * U-E.1: Verify U-A handler wires Layer 1 physical fields + Layer 2 recalculate correctly.
 *
 * Flow:
 *   createServerPool -> createPhysicalServer -> attachPhysicalServerRole(KVM_HOST)
 *   (which drives addHost -> KVM agent connect -> ReportHostCapacityMessage)
 *   -> PSC.totalCpu / totalMemory reflect simulator values
 *   -> PSC.availableCpu = totalCpu - buffer (not 0, not totalCpu)
 *
 * 12a rule: no dbf.persist / SQL insert in test* body; all state via production API.
 */
class KvmReportHostCapacityRecalcCase extends SubCase {

    static final int SIM_CPU_NUM    = 16
    static final long SIM_TOTAL_MEM = SizeUnit.GIGABYTE.toByte(64)

    EnvSpec env

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = HostEnv.noHostBasicEnv()
    }

    @Override
    void test() {
        env.create {
            env.afterSimulator(KVM_HOST_FACT_PATH) { rsp -> rsp }

            env.afterSimulator(KVMConstant.KVM_HOST_CAPACITY_PATH) { rsp, HttpEntity<String> e ->
                rsp = new KVMAgentCommands.HostCapacityResponse()
                rsp.success = true
                rsp.cpuNum = SIM_CPU_NUM
                rsp.totalMemory = SIM_TOTAL_MEM
                rsp.usedCpu = 0
                rsp.usedMemory = 0
                rsp.cpuSpeed = 1
                rsp.cpuSockets = 2
                rsp.cpuCoreNum = 8
                return rsp
            }

            testPscPopulatedAfterReportCapacity()
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    void testPscPopulatedAfterReportCapacity() {
        def zone    = env.inventoryByName("zone")    as ZoneInventory
        def cluster = env.inventoryByName("cluster") as ClusterInventory

        ServerPoolInventory pool = createServerPool {
            name = "pool-recalc"
            delegate.zoneUuid = zone.uuid
        } as ServerPoolInventory

        PhysicalServerInventory server = createPhysicalServer {
            name = "server-recalc"
            delegate.zoneUuid = zone.uuid
            delegate.poolUuid = pool.uuid
            managementIp = "127.0.1.1"
        } as PhysicalServerInventory

        attachPhysicalServerRole {
            serverUuid = server.uuid
            roleType   = "KVM_HOST"
            clusterUuid = cluster.uuid
            roleConfig = [username: "root", password: "password", sshPort: "22"]
        }

        String serverUuid = Q.New(PhysicalServerRoleVO.class)
                .eq(PhysicalServerRoleVO_.serverUuid, server.uuid)
                .eq(PhysicalServerRoleVO_.roleType, "KVM_HOST")
                .select(PhysicalServerRoleVO_.serverUuid)
                .findValue()
        assert serverUuid != null : "PhysicalServerRoleVO not found for server=${server.uuid}"

        // ReportHostCapacityMessage is sent asynchronously after host connect;
        // poll until PSC row is written and recalculate has run.
        retryInSecs {
            PhysicalServerCapacityVO psc = Q.New(PhysicalServerCapacityVO.class)
                    .eq(PhysicalServerCapacityVO_.uuid, serverUuid)
                    .find()

            assert psc != null : "PSC row missing for serverUuid=${serverUuid}"

            // Layer 1: physical quantities from simulator.
            // KVM path: PSC.totalCpu = cpuNum × HostCpuOverProvisioningRatio
            // (HostAllocatorManagerImpl.handle(ReportHostCapacityMessage) line 193 →
            //  cpuRatioMgr.calculateHostCpuByRatio). Default ratio is 10 but resource
            // configs / global tweaks could change it; read live to stay correct.
            int cpuRatio = HostGlobalConfig.HOST_CPU_OVER_PROVISIONING_RATIO.value(Integer.class)
            long expectedTotalCpu = SIM_CPU_NUM * cpuRatio
            assert psc.totalCpu == expectedTotalCpu :
                    "PSC.totalCpu expected ${expectedTotalCpu} (cpuNum=${SIM_CPU_NUM} × ratio=${cpuRatio}) got ${psc.totalCpu}"
            assert psc.totalMemory == SIM_TOTAL_MEM :
                    "PSC.totalMemory expected ${SIM_TOTAL_MEM} got ${psc.totalMemory}"

            // Layer 2: availableCpu must reflect recalculate having run. _recalculate
            // no longer subtracts an implicit safety buffer (each role self-reports via
            // ServerReservedCapacityExtensionPoint); for a fresh KVM-only host with 0
            // consumed and no ext reservation, availableCpu == totalCpu.
            assert psc.availableCpu == psc.totalCpu :
                    "PSC.availableCpu should equal totalCpu (no ext reservation, no consumed)" +
                    " for fresh KVM host, got availableCpu=${psc.availableCpu} totalCpu=${psc.totalCpu}"
        }

        // Cleanup. Use delegate.serverUuid to avoid the Groovy DSL closure trap:
        // local `String serverUuid` (defined above) shadows the SDK action's setter,
        // and `serverUuid = server.uuid` would reassign the local variable instead of
        // populating the API message — leaving message.serverUuid null.
        // (Same playbook §5 caveat as chassisUuid = chassisUuid in BM2 fixtures.)
        detachPhysicalServerRole {
            delegate.serverUuid = server.uuid
            roleType = "KVM_HOST"
        }
        deletePhysicalServer { uuid = server.uuid }
        deleteServerPool { uuid = pool.uuid }
    }
}
