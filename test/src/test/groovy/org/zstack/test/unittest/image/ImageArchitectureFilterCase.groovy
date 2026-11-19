package org.zstack.test.unittest.image

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.stubbing.Answer
import org.zstack.core.Platform
import org.zstack.header.allocator.HostAllocatorSpec
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.OperationFailureException
import org.zstack.header.host.HostVO
import org.zstack.header.image.ImageInventory
import org.zstack.header.vm.VmInstanceInventory
import org.zstack.image.ImageManagerImpl

import static org.mockito.Mockito.mockStatic

class ImageArchitectureFilterCase {
    private MockedStatic<Platform> platform
    private ImageManagerImpl manager

    @Before
    void setUp() {
        Platform.getUuid()
        platform = mockStatic(Platform, { invocation ->
            if (invocation.method.name == 'operr') {
                return new ErrorCode(code: 'SYS.1006', globalErrorCode: invocation.arguments[0],
                        details: String.format(invocation.arguments[1] as String,
                                invocation.arguments.drop(2) as Object[]))
            }
            return invocation.arguments[0]
        } as Answer)
        manager = new ImageManagerImpl()
    }

    @After
    void tearDown() {
        platform?.close()
    }

    private void assertMismatch(HostAllocatorSpec spec) {
        try {
            manager.filterHostCandidates([new HostVO(architecture: 'x86_64')], spec)
            assert false: 'An incompatible image must report both architectures'
        } catch (OperationFailureException error) {
            assert error.errorCode.globalErrorCode == 'ORG_ZSTACK_IMAGE_10049'
            assert error.errorCode.details.contains('aarch64')
            assert error.errorCode.details.contains('x86_64')
            assert error.errorCode.details.contains('same architecture')
        }
    }

    @Test
    void mismatchReportsArchitecturesForImageAndExistingVm() {
        assertMismatch(new HostAllocatorSpec(image: new ImageInventory(architecture: 'aarch64')))
        assertMismatch(new HostAllocatorSpec(vmInstance: new VmInstanceInventory(architecture: 'aarch64')))
    }

    @Test
    void explicitArchitectureHasPriorityOverImageAndVm() {
        assertMismatch(new HostAllocatorSpec(architecture: 'aarch64',
                image: new ImageInventory(architecture: 'x86_64'),
                vmInstance: new VmInstanceInventory(architecture: 'x86_64')))
    }

    @Test
    void imageArchitectureHasPriorityOverExistingVm() {
        assertMismatch(new HostAllocatorSpec(image: new ImageInventory(architecture: 'aarch64'),
                vmInstance: new VmInstanceInventory(architecture: 'x86_64')))
    }

    @Test
    void mixedCandidatesKeepCompatibleHosts() {
        def matching = new HostVO(architecture: 'aarch64')
        def candidates = [new HostVO(architecture: 'x86_64'), matching]
        assert manager.filterHostCandidates(candidates, new HostAllocatorSpec(architecture: 'aarch64')) == [matching]
        assert candidates.size() == 2
    }

    @Test
    void absentArchitectureKeepsCandidates() {
        def candidates = [new HostVO(architecture: 'x86_64')]
        assert manager.filterHostCandidates(candidates, new HostAllocatorSpec()).is(candidates)
    }

    @Test
    void emptyCandidatesDoNotInventArchitectureMismatch() {
        assert manager.filterHostCandidates([], new HostAllocatorSpec(architecture: 'aarch64')).isEmpty()
    }

    @Test
    void fallbackReasonIsActionableWithoutRequestState() {
        assert manager.filterErrorReason()?.contains('cpu architecture')
    }
}
