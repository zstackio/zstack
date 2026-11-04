package org.zstack.test.unittest.compute

import org.junit.Test
import org.mockito.Answers
import org.mockito.stubbing.Answer
import org.zstack.compute.vm.VmInstanceBase
import org.zstack.core.Platform
import org.zstack.core.workflow.FlowChainBuilder
import org.zstack.header.core.Completion
import org.zstack.header.core.workflow.FlowChain
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.message.Message
import org.zstack.header.vm.*

import static org.mockito.Mockito.*

class VmMigrationStateValidationCase {
    static class QueuedVm extends VmInstanceBase {
        VmInstanceVO beforeQueue
        VmInstanceVO queuedState
        boolean refreshed
        boolean validated

        QueuedVm() { super(new VmInstanceVO(uuid: 'vm')) }

        @Override
        protected VmInstanceVO refreshVO() {
            refreshed = true
            self = queuedState
            return self
        }

        @Override
        ErrorCode validateOperationByState(Message message, VmInstanceState state, Enum error) {
            assert refreshed
            validated = true
            return super.validateOperationByState(message, state, error)
        }

        void migrate(MigrateVmMsg message, Completion completion) {
            self = beforeQueue
            super.migrateVm(message, completion)
        }
    }

    @Test
    void testQueuedUnknownUsesExistingMigrationRules() {
        exerciseStateChange(VmInstanceState.Running, VmInstanceState.Unknown, false)
    }

    @Test
    void testRunningAndPausedUseExistingMigrationRules() {
        [VmInstanceState.Running, VmInstanceState.Paused].each {
            exerciseStateChange(it, it, false)
        }
    }

    @Test
    void testQueuedInvalidStatesStillFailExistingValidation() {
        [VmInstanceState.Stopped, VmInstanceState.Migrating, VmInstanceState.VolumeMigrating,
         VmInstanceState.Destroyed, VmInstanceState.Starting].each {
            exerciseStateChange(VmInstanceState.Running, it, true)
        }
    }

    @Test
    void testQueuedValidationUsesRefreshedStateInsteadOfSubmissionState() {
        exerciseStateChange(VmInstanceState.Migrating, VmInstanceState.Running, false)
    }

    private static void exerciseStateChange(VmInstanceState initialState, VmInstanceState refreshedState, boolean blocked) {
        def failure = new ErrorCode(code: 'TEST.STATE', details: 'migration rejected by existing state validation')
        def platform = mockStatic(Platform, withSettings().mockMaker('mock-maker-inline').defaultAnswer({ invocation ->
            invocation.method.name == 'err' ? failure : Answers.RETURNS_DEFAULTS.answer(invocation)
        } as Answer))
        FlowChain chain = mock(FlowChain, Answers.RETURNS_SELF)
        def builder = mockStatic(FlowChainBuilder, withSettings().mockMaker('mock-maker-inline'))
        builder.when({ FlowChainBuilder.newSimpleFlowChain() }).thenReturn(chain)
        try {
            // Exercise the actual migrateVm method and existing state table after refresh.
            QueuedVm vm = mock(QueuedVm, Answers.CALLS_REAL_METHODS)
            vm.beforeQueue = new VmInstanceVO(uuid: 'vm', state: initialState, internalId: 1L)
            vm.queuedState = new VmInstanceVO(uuid: 'vm', state: refreshedState, internalId: 1L)
            Completion completion = mock(Completion)
            vm.migrate(new MigrateVmMsg(vmInstanceUuid: 'vm'), completion)
            assert vm.refreshed && vm.validated
            if (blocked) {
                verify(completion).fail(failure)
                builder.verifyNoInteractions()
                verify(chain, never()).start()
            } else {
                verify(completion, never()).fail(any(ErrorCode))
                verify(chain).start()
            }
        } finally {
            builder.close()
            platform.close()
        }
    }
}
