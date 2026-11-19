package org.zstack.test.unittest.compute

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Answers
import org.mockito.MockedStatic
import org.mockito.stubbing.Answer
import org.zstack.compute.vm.VmAllocateHostAndPrimaryStorageFlow
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.workflow.FlowChainBuilder
import org.zstack.header.core.workflow.FlowChain
import org.zstack.header.core.workflow.FlowDoneHandler
import org.zstack.header.core.workflow.FlowErrorHandler
import org.zstack.header.core.workflow.FlowTrigger
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.ErrorCodeList
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.vm.VmInstanceConstant
import org.zstack.header.vm.VmInstanceInventory
import org.zstack.header.vm.VmInstanceSpec

import javax.persistence.Tuple

import static org.mockito.Mockito.*

class VmAllocationFailureAggregationCase {
    private MockedStatic<Q> queries
    private MockedStatic<SQL> sql
    private MockedStatic<FlowChainBuilder> builder
    private List<String> available = ['local', 'nfs']
    private List<ErrorCode> outcomes = []
    private List<List<String>> attempted = []
    private VmInstanceSpec spec
    private VmAllocateHostAndPrimaryStorageFlow flow
    private FlowTrigger trigger
    private ErrorCode failure
    private int nextCount

    @Before
    void setUp() {
        Q query = mock(Q, { invocation ->
            if (invocation.method.name == 'listValues') {
                return ['LocalStorage', 'NFS']
            }
            if (invocation.method.name == 'listTuple') {
                return available.collect { uuid ->
                    Tuple tuple = mock(Tuple)
                    when(tuple.get(0)).thenReturn(uuid)
                    when(tuple.get(1)).thenReturn(uuid == 'local' ? 'LocalStorage' : 'NFS')
                    tuple
                }
            }
            Answers.RETURNS_SELF.answer(invocation)
        } as Answer)
        queries = mockStatic(Q)
        queries.when({ Q.New(PrimaryStorageVO) } as MockedStatic.Verification).thenReturn(query)
        SQL storageQuery = mock(SQL, { invocation ->
            invocation.method.name == 'list' ? ['local', 'nfs'] : Answers.RETURNS_SELF.answer(invocation)
        } as Answer)
        sql = mockStatic(SQL, { storageQuery } as Answer)

        spec = new VmInstanceSpec(vmInventory: new VmInstanceInventory(uuid: 'vm', clusterUuid: 'cluster'),
                imageSpec: mock(VmInstanceSpec.ImageSpec))
        flow = new VmAllocateHostAndPrimaryStorageFlow()
        flow.@dbf = mock(DatabaseFacade, { invocation ->
            new PrimaryStorageVO(uuid: invocation.arguments[0] as String)
        } as Answer)
        flow.@pluginRgty = mock(PluginRegistry, { [] } as Answer)
        flow.@errf = mock(ErrorFacade, { invocation ->
            assert invocation.method.name == 'instantiateErrorCode'
            assert invocation.arguments[0] == SysErrors.OPERATION_ERROR
            new ErrorCodeList(code: 'SYS.1006', details: invocation.arguments[1], causes: invocation.arguments[2])
        } as Answer)
        trigger = mock(FlowTrigger, { invocation ->
            if (invocation.method.name == 'fail') {
                failure = invocation.arguments[0] as ErrorCode
            } else if (invocation.method.name == 'next') {
                nextCount++
            }
            null
        } as Answer)
        builder = mockStatic(FlowChainBuilder, { invocation ->
            FlowDoneHandler done
            FlowErrorHandler error
            Map chainData = [:]
            mock(FlowChain, { call ->
                switch (call.method.name) {
                    case 'getData': return chainData
                    case 'done': done = call.arguments[0] as FlowDoneHandler; break
                    case 'error': error = call.arguments[0] as FlowErrorHandler; break
                    case 'start':
                        attempted.add([spec.requiredPrimaryStorageUuidForRootVolume,
                                       spec.requiredPrimaryStorageUuidForDataVolume])
                        ErrorCode result = outcomes[attempted.size() - 1]
                        if (result == null) {
                            done.handle(chainData)
                        } else {
                            error.handle(result, chainData)
                        }
                        break
                }
                Answers.RETURNS_SELF.answer(call)
            } as Answer)
        } as Answer)
    }

    @After
    void tearDown() {
        builder?.close()
        sql?.close()
        queries?.close()
    }

    private void allocate() {
        flow.run(trigger, [(VmInstanceConstant.Params.VmInstanceSpec.toString()): spec])
    }

    @Test
    void allStorageFailuresKeepCapacityAndPciCauses() {
        outcomes = [new ErrorCode(code: 'ALLOC.CAPACITY', details: 'insufficient primary storage capacity'),
                    new ErrorCode(code: 'ALLOC.PCI', details: 'PciDeviceFilterFlow: no accessible device')]
        allocate()
        assert attempted == [['local', null], ['nfs', null]]
        assert nextCount == 0
        assert failure instanceof ErrorCodeList
        assert failure.globalErrorCode == 'ORG_ZSTACK_COMPUTE_VM_10351'
        assert failure.code == 'SYS.1006'
        assert (failure as ErrorCodeList).causes == outcomes
        assert (failure as ErrorCodeList).causes[0].is(outcomes[0])
        assert (failure as ErrorCodeList).causes[1].is(outcomes[1])
        assert failure.details.contains(outcomes[0].details)
        assert failure.details.contains(outcomes[1].details)
        assert failure.formatArgs.toList() == [failure.details]
    }

    @Test
    void oneAvailableCombinationPreservesOriginalError() {
        available = ['local']
        outcomes = [new ErrorCode(code: 'ALLOC.CAPACITY', globalErrorCode: 'CAPACITY.ORIGINAL', details: 'full')]
        allocate()
        assert attempted == [['local', null]]
        assert nextCount == 0
        assert failure.is(outcomes[0])
        verifyNoInteractions(flow.@errf)
    }

    @Test
    void laterSuccessfulStorageDoesNotFailWithEarlierError() {
        outcomes = [new ErrorCode(code: 'ALLOC.CAPACITY', details: 'full'), null]
        allocate()
        assert attempted == [['local', null], ['nfs', null]]
        assert nextCount == 1
        assert failure == null
        verifyNoInteractions(flow.@errf)
    }

    @Test
    void firstSuccessfulStorageStopsRetrying() {
        outcomes = [null]
        allocate()
        assert attempted == [['local', null]]
        assert nextCount == 1
        assert failure == null
        verifyNoInteractions(flow.@errf)
    }
}
