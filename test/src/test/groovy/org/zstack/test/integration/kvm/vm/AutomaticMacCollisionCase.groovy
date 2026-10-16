package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.MacOperator
import org.zstack.compute.vm.VmGlobalProperty
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.db.Q
import org.zstack.header.vm.VmNicVO
import org.zstack.header.vm.VmNicVO_
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.sdk.CreateVmInstanceAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.kvm.host.HostEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.InstanceOfferingSpec
import org.zstack.testlib.KVMHostSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.atomic.AtomicReference

/**
 * Scenario: Occupy deterministic automatic MAC candidates through VM creation, then create another VM.
 * Expected: Ip allocation tries at most five candidates; Random and custom duplicates still fail.
 */
class AutomaticMacCollisionCase extends SubCase {
    private static final long RETRY_SEED = 1700000001L

    EnvSpec env
    String l3Uuid
    Random macRandom
    String originalSchema
    String originalConsoleIp
    String originalConsoleIpv4
    List<String> retryCandidates
    VmInstanceInventory lastCandidateVm
    AtomicReference<Map> start = new AtomicReference<>()

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        originalSchema = VmGlobalProperty.vmMacAddressSchema
        originalConsoleIp = CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP
        originalConsoleIpv4 = CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IPV4
        env = HostEnv.oneHostEnv()
        InstanceOfferingSpec offering = env.find("instanceOffering", InstanceOfferingSpec.class)
        offering.cpu = 1
        offering.memory = SizeUnit.MEGABYTE.toByte(512)
        KVMHostSpec host = env.find("kvm", KVMHostSpec.class)
        host.totalCpu = 64
        host.totalMem = SizeUnit.GIGABYTE.toByte(64)
    }

    @Override
    void test() {
        env.create {}
        l3Uuid = env.inventoryByName("l3").uuid
        def randomField = MacOperator.class.getDeclaredField("random")
        randomField.accessible = true
        macRandom = randomField.get(null) as Random

        env.afterSimulator(KVMConstant.KVM_START_VM_PATH) { rsp, HttpEntity<String> request ->
            def cmd = JSONObjectUtil.toObject(request.body, KVMAgentCommands.StartVmCmd.class)
            start.set([command: cmd, response: rsp])
            return rsp
        }

        VmGlobalProperty.vmMacAddressSchema = "Ip"
        CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP = "172.24.3.81"
        CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IPV4 = "172.24.3.81"

        testFirstCollisionRecovers()
        testFifthCandidateSucceeds()
        testExhaustedCandidatesAreRejected()
        testCustomMacIsNotReplaced()
        testRandomCollisionIsRejected()
    }

    @Override
    void clean() {
        VmGlobalProperty.vmMacAddressSchema = originalSchema
        CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IP = originalConsoleIp
        CoreGlobalProperty.CONSOLE_PROXY_OVERRIDDEN_IPV4 = originalConsoleIpv4
        macRandom?.setSeed(new Random().nextLong())
        env.delete()
    }

    void testFirstCollisionRecovers() {
        long seed = 12L
        List<String> candidates = predictCandidates(seed, 2)
        createVm("first-candidate-owner", candidates[0])

        macRandom.setSeed(seed)
        start.set(null)
        VmInstanceInventory vm = createVm("second-candidate-vm")

        assertAllocatedMac(vm, candidates[1])
        // This seed also checks leading zeroes without deriving the expected bytes from the generator.
        assert vm.vmNics[0].mac == "fa:03:51:0f:50:00" : "IP MAC mismatch: expected=fa:03:51:0f:50:00, actual=${vm.vmNics[0].mac}"
    }

    void testFifthCandidateSucceeds() {
        retryCandidates = predictCandidates(RETRY_SEED, 6)
        retryCandidates.take(4).eachWithIndex { String mac, int index ->
            createVm("retry-candidate-owner-${index}", mac)
        }

        macRandom.setSeed(RETRY_SEED)
        start.set(null)
        lastCandidateVm = createVm("fifth-candidate-vm")
        assertAllocatedMac(lastCandidateVm, retryCandidates[4])
    }

    void testExhaustedCandidatesAreRejected() {
        // The successful VM now occupies candidate five; candidate six must remain unused.
        assert !Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3Uuid).eq(VmNicVO_.mac, retryCandidates[5]).isExists() : "sixth candidate must be free: mac=${retryCandidates[5]}"
        long nicCount = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3Uuid).count()
        macRandom.setSeed(RETRY_SEED)
        start.set(null)

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "exhausted-candidates-vm"
        action.imageUuid = env.inventoryByName("image1").uuid
        action.instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
        action.l3NetworkUuids = [l3Uuid]
        action.sessionId = adminSession()
        def result = action.call()

        assert result.error?.cause?.globalErrorCode == "ORG_ZSTACK_COMPUTE_VM_10069" : "exhausted candidates must fail: expected=ORG_ZSTACK_COMPUTE_VM_10069, actual=${result.error}"
        long actualCount = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3Uuid).count()
        assert actualCount == nicCount : "NIC count mismatch: expected=${nicCount}, actual=${actualCount}"
        assert start.get() == null : "rejected VM must not start: actual=${start.get()}"
    }

    void testCustomMacIsNotReplaced() {
        // Exhaustion leaves the free sixth candidate next; an explicit MAC must not be replaced with it.
        String customMac = lastCandidateVm.vmNics[0].mac
        long nicCount = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3Uuid).count()
        start.set(null)

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "custom-duplicate-vm"
        action.imageUuid = env.inventoryByName("image1").uuid
        action.instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
        action.l3NetworkUuids = [l3Uuid]
        action.systemTags = ["customMac::${l3Uuid}::${customMac}".toString()]
        action.sessionId = adminSession()
        def result = action.call()

        assert result.error?.cause?.globalErrorCode == "ORG_ZSTACK_COMPUTE_VM_10069" : "custom duplicate must fail: expected=ORG_ZSTACK_COMPUTE_VM_10069, actual=${result.error}"
        long actualCount = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3Uuid).count()
        assert actualCount == nicCount : "NIC count mismatch: expected=${nicCount}, actual=${actualCount}"
        assert start.get() == null : "rejected VM must not start: actual=${start.get()}"
    }

    void testRandomCollisionIsRejected() {
        VmGlobalProperty.vmMacAddressSchema = "Random"
        long seed = 987654321L
        List<String> candidates = predictCandidates(seed, 2)
        createVm("random-candidate-owner", candidates[0])

        assert !Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3Uuid).eq(VmNicVO_.mac, candidates[1]).isExists() : "second candidate must be free: mac=${candidates[1]}"
        long nicCount = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3Uuid).count()
        macRandom.setSeed(seed)
        start.set(null)

        CreateVmInstanceAction action = new CreateVmInstanceAction()
        action.name = "random-collision-vm"
        action.imageUuid = env.inventoryByName("image1").uuid
        action.instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
        action.l3NetworkUuids = [l3Uuid]
        action.sessionId = adminSession()
        def result = action.call()

        assert result.error?.cause?.globalErrorCode == "ORG_ZSTACK_COMPUTE_VM_10069" : "Random collision must fail: expected=ORG_ZSTACK_COMPUTE_VM_10069, actual=${result.error}"
        long actualCount = Q.New(VmNicVO.class).eq(VmNicVO_.l3NetworkUuid, l3Uuid).count()
        assert actualCount == nicCount : "NIC count mismatch: expected=${nicCount}, actual=${actualCount}"
        assert start.get() == null : "rejected VM must not start: actual=${start.get()}"
    }

    private List<String> predictCandidates(long seed, int count) {
        // Java static calls use the real generator; resetting its seed repeats the candidate sequence.
        macRandom.setSeed(seed)
        List<String> candidates = (0..<count).collect { MacOperator.generateMacWithDeviceId((short) 0) }
        assert candidates.toSet().size() == count : "candidate count mismatch: expected=${count}, actual=${candidates}"
        return candidates
    }

    private VmInstanceInventory createVm(String vmName, String mac = null) {
        return createVmInstance {
            name = vmName
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [l3Uuid]
            if (mac != null) {
                systemTags = ["customMac::${l3Uuid}::${mac}".toString()]
            }
        }
    }

    private void assertAllocatedMac(VmInstanceInventory vm, String expectedMac) {
        assert vm.vmNics.size() == 1 : "NIC count mismatch: expected=1, actual=${vm.vmNics.size()}"
        def nic = vm.vmNics[0]
        assert nic.mac == expectedMac : "API MAC mismatch: expected=${expectedMac}, actual=${nic.mac}"
        VmNicVO persisted = Q.New(VmNicVO.class).eq(VmNicVO_.uuid, nic.uuid).find()
        assert persisted?.mac == expectedMac : "persisted MAC mismatch: expected=${expectedMac}, actual=${persisted?.mac}"
        retryInSecs {
            def observed = start.get()
            assert observed?.response?.success : "VM start failed: actual=${observed?.response?.success}, error=${observed?.response?.error}"
            assert observed.command.vmInstanceUuid == vm.uuid : "started VM mismatch: expected=${vm.uuid}, actual=${observed.command.vmInstanceUuid}"
            def hostNic = observed.command.nics.find { it.uuid == nic.uuid }
            assert hostNic?.mac == expectedMac : "host MAC mismatch: expected=${expectedMac}, actual=${hostNic?.mac}"
            assert hostNic.deviceId == nic.deviceId : "host device ID mismatch: expected=${nic.deviceId}, actual=${hostNic.deviceId}"
        }
    }
}
