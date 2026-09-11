package org.zstack.test.unittest.compute

import org.junit.Test
import org.zstack.compute.vm.VmInstanceExtensionPointEmitter
import org.zstack.header.core.Completion
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint

class MigrateVmFinalizeCompletionCase {
    @Test
    void testSuccessThenExceptionWhileNextExtensionIsPending() {
        verifyFirstCompletion(false, true)
    }

    @Test
    void testSuccessThenFailureWhileNextExtensionIsPending() {
        verifyFirstCompletion(false, false)
    }

    @Test
    void testFailureThenSuccessKeepsOriginalError() {
        verifyFirstCompletion(true, false)
    }

    private static void verifyFirstCompletion(boolean failFirst, boolean throwAfterSuccess) {
        ErrorCode firstError = new ErrorCode(code: 'TEST.FIRST', description: 'first result')
        ErrorCode lateError = new ErrorCode(code: 'TEST.LATE', description: 'late result')
        Completion pending
        int nextCalls = 0
        List<ErrorCode> results = []
        def first = new VmInstanceMigrateExtensionPoint() {
            @Override
            void beforeMigrateVm(VmInstanceInventory inventory, String hostUuid) {}

            @Override
            void finalizeMigrateVm(VmInstanceInventory inventory, String hostUuid, Completion completion) {
                if (failFirst) {
                    completion.fail(firstError)
                    completion.success()
                } else {
                    completion.success()
                    if (throwAfterSuccess) {
                        throw new OperationFailureException(lateError)
                    }
                    completion.fail(lateError)
                }
            }
        }
        def second = new VmInstanceMigrateExtensionPoint() {
            @Override
            void beforeMigrateVm(VmInstanceInventory inventory, String hostUuid) {}

            @Override
            void finalizeMigrateVm(VmInstanceInventory inventory, String hostUuid, Completion completion) {
                nextCalls++
                pending = completion
            }
        }
        def emitter = new VmInstanceExtensionPointEmitter()
        emitter.migrateVmExtensions = [first, second]
        emitter.finalizeMigrateVm(new VmInstanceInventory(uuid: 'vm', hostUuid: 'target'), 'source',
                new Completion(null) {
                    @Override
                    void success() { results.add(null) }

                    @Override
                    void fail(ErrorCode error) { results.add(error) }
                })
        assert nextCalls == 1
        assert pending != null
        assert results.empty
        pending.success()
        assert results.size() == 1
        assert results[0].is(failFirst ? firstError : null)
    }
}
