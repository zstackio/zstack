package org.zstack.test.storage.primary.local;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostAllocatorTrigger;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.AllocatePrimaryStorageForVmMigrationFlow;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by david on 2/9/17.
 */
public class TestAllocatePsFlow {

    @InjectMocks
    private AllocatePrimaryStorageForVmMigrationFlow allocateFlow;

    @Mock
    private DatabaseFacade dbf;

    @Mock
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Before
    public void setUp() {
        allocateFlow = new AllocatePrimaryStorageForVmMigrationFlow();
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testFirstFlowException() {
        // There is no other flows - we the first flow.
        allocateFlow.allocate();
    }

    @Test(expected = CloudRuntimeException.class)
    public void testInvalidVmOperation() {
        HostVO hvo = new HostVO();
        allocateFlow.setCandidates(Collections.singletonList(hvo));

        HostAllocatorSpec spec = new HostAllocatorSpec();
        spec.setVmOperation(VmInstanceConstant.VmOperation.Destroy.toString());
        allocateFlow.setSpec(spec);

        allocateFlow.allocate();
    }

    private HostAllocatorTrigger prepareAllocateParams(long volSize, long availableCapacitity) {
        HostVO hvo = new HostVO();
        hvo.setUuid(Platform.getUuid());
        allocateFlow.setCandidates(Collections.singletonList(hvo));

        HostAllocatorSpec spec = new HostAllocatorSpec();
        spec.setVmOperation(VmInstanceConstant.VmOperation.Migrate.toString());

        VmInstanceInventory vm = new VmInstanceInventory();
        List<VolumeInventory> vols = new ArrayList<>();
        VolumeInventory vol = new VolumeInventory();
        vol.setSize(volSize);
        vol.setUuid(Platform.getUuid());
        vols.add(vol);
        vm.setRootVolumeUuid(vol.getUuid());
        vm.setAllVolumes(vols);

        spec.setVmInstance(vm);

        // Mock the dbf query against VolumeSnapshotVO
        SimpleQuery<VolumeSnapshotVO> sq = Mockito.mock(SimpleQuery.class);
        Mockito.when(sq.listValue()).thenReturn(new ArrayList<>());

        Mockito.doAnswer(new Answer<SimpleQuery<VolumeSnapshotVO>>() {
            @Override
            public SimpleQuery<VolumeSnapshotVO> answer(InvocationOnMock invocation) throws Throwable {
                return sq;
            }
        }).when(dbf).createQuery(VolumeSnapshotVO.class);

        // Mock the dbf query against LocalStorageHostRefVO
        SimpleQuery<LocalStorageHostRefVO> q = Mockito.mock(SimpleQuery.class);
        LocalStorageHostRefVO href = new LocalStorageHostRefVO();
        href.setHostUuid(hvo.getUuid());
        href.setAvailableCapacity(availableCapacitity);
        Mockito.when(q.list()).thenReturn(Collections.singletonList(href));

        Mockito.doAnswer(new Answer<SimpleQuery<LocalStorageHostRefVO>>() {
            @Override
            public SimpleQuery<LocalStorageHostRefVO> answer(InvocationOnMock invocation) throws Throwable {
                return q;
            }
        }).when(dbf).createQuery(LocalStorageHostRefVO.class);

        // Mock the ratioManager
        Mockito.when(ratioMgr.calculateByRatio(Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(vol.getSize());

        // Mock the trigger
        HostAllocatorTrigger trigger = Mockito.mock(HostAllocatorTrigger.class);
        allocateFlow.setTrigger(trigger);

        allocateFlow.setSpec(spec);
        allocateFlow.allocate();

        return trigger;
    }

    @Test
    public void testAllocateSuccess() {
        long volSize = 5678;
        HostAllocatorTrigger trigger = prepareAllocateParams(volSize, volSize+1);
        Mockito.verify(trigger).next(Mockito.anyList());
    }

    @Test(expected = OperationFailureException.class)
    public void testAllocateFailure() {
        long volSize = 5678;
        HostAllocatorTrigger trigger = prepareAllocateParams(volSize, volSize);

        // In fact, the verification below will never be called due to OpFailure.
        Mockito.verify(trigger, Mockito.never()).next(Mockito.anyList());
    }
}
