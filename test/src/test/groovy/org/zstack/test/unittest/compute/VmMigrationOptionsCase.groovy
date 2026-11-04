package org.zstack.test.unittest.compute

import org.junit.Test
import org.mockito.ArgumentCaptor
import org.zstack.compute.vm.VmMigrateOnHypervisorFlow
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.CloudBusCallBack
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.host.HostConstant
import org.zstack.header.host.HostInventory
import org.zstack.header.host.MigrateVmOnHypervisorMsg
import org.zstack.header.message.Message
import org.zstack.header.message.MessageReply
import org.zstack.header.message.NeedReplyMessage
import org.zstack.header.vm.MigrateVmInnerMsg
import org.zstack.header.vm.MigrateVmMsg
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.vm.VmInstanceSpec
import org.zstack.longjob.LongJobUtils

import static org.mockito.Mockito.*

class VmMigrationOptionsCase {
    @Test
    void testExplicitOptionsReachHypervisor() {
        long bandwidth = 8L * 1024 * 1024 * 1024
        MigrateVmOnHypervisorMsg sent = runFlow(new MigrateVmMsg(
                strategy: 'auto-converge', downTime: 321, bandwidth: bandwidth,
                migrateFromDestination: true))

        assert sent.strategy == 'auto-converge'
        assert sent.downTime == 321
        assert sent.bandwidth == bandwidth
        assert sent.migrateFromDestination
        assert sent.hostUuid == 'target'
    }

    @Test
    void testUnsetOptionsKeepDefaults() {
        MigrateVmOnHypervisorMsg sent = runFlow(new MigrateVmMsg())

        assert sent.strategy == null
        assert sent.downTime == null
        assert sent.bandwidth == 0L
        assert !sent.migrateFromDestination
        assert sent.hostUuid == 'source'
    }

    @Test
    void testLegacyMessageKeepsZeroBandwidth() {
        MigrateVmOnHypervisorMsg sent = runFlow(new MigrateVmInnerMsg(strategy: 'auto-converge'))

        assert sent.strategy == 'auto-converge'
        assert sent.bandwidth == 0L
    }

    @Test
    void testOtherMessagesKeepZeroBandwidth() {
        MigrateVmOnHypervisorMsg sent = runFlow(new NeedReplyMessage() {})

        assert sent.strategy == null
        assert sent.downTime == null
        assert sent.bandwidth == 0L
        assert !sent.migrateFromDestination
    }

    private static MigrateVmOnHypervisorMsg runFlow(Message message) {
        CloudBus bus = mock(CloudBus)
        FlowTrigger trigger = mock(FlowTrigger)
        VmMigrateOnHypervisorFlow flow = new VmMigrateOnHypervisorFlow()
        flow.bus = bus
        VmInstanceSpec spec = new VmInstanceSpec(message: message,
                vmInventory: new VmInstanceInventory(uuid: 'vm', hostUuid: 'source'),
                destHost: new HostInventory(uuid: 'target'))

        // Isolate the cancellation DB lookup while exercising the actual flow and outgoing message.
        def longJob = mockStatic(LongJobUtils, withSettings().mockMaker('mock-maker-inline'))
        try {
            flow.run(trigger, [(VmInstanceConstant.Params.VmInstanceSpec.toString()): spec])
        } finally {
            longJob.close()
        }

        ArgumentCaptor<MigrateVmOnHypervisorMsg> sent = ArgumentCaptor.forClass(MigrateVmOnHypervisorMsg)
        ArgumentCaptor<CloudBusCallBack> callback = ArgumentCaptor.forClass(CloudBusCallBack)
        verify(bus).send((NeedReplyMessage) sent.capture(), callback.capture())
        verify(bus).makeTargetServiceIdByResourceUuid(sent.value, HostConstant.SERVICE_ID, sent.value.hostUuid)
        verify(trigger, never()).next()
        assert sent.value.vmInventory.is(spec.vmInventory)
        assert sent.value.destHostInventory.is(spec.destHost)
        assert sent.value.srcHostUuid == 'source'
        callback.value.run(new MessageReply())
        verify(trigger).next()
        return sent.value
    }
}
