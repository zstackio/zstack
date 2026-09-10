package org.zstack.kvm.hypervisor;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO;

import javax.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * The hypervisor info of the same host/vm uuid is reported by several asynchronous paths
 * (the StartVm response and the libvirtReportStart event when a vm starts). save() checks
 * then inserts, so concurrent paths both see the row as absent and the later transaction
 * violates the primary key of KvmHypervisorInfoVO; the insert must fall back to an update.
 */
public class KvmHypervisorInfoManagerConcurrentInsertTest {
    private static final String VM_UUID = "4a6173618107489e8013f294e37a533d";
    private static final String HOST_UUID = "61150909000040008000000000000011";

    @Mock
    private DatabaseFacade db;
    @InjectMocks
    private KvmHypervisorInfoManagerImpl manager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void insertsWhenNoOtherTransactionWroteTheRow() {
        KvmHypervisorInfoVO info = hypervisorInfo();

        manager.saveNewHypervisorInfoList(Collections.singletonList(info));

        verify(db).persistCollection(Collections.singletonList(info));
        verify(db, never()).updateCollection(anyCollection());
    }

    @Test
    public void fallsBackPerUuidWhenTheBatchHitsAConcurrentInsert() {
        KvmHypervisorInfoVO concurrent = hypervisorInfo(VM_UUID);
        KvmHypervisorInfoVO missing = hypervisorInfo(HOST_UUID);
        doThrow(duplicateEntry()).when(db).persistCollection(anyCollection());
        doReturn(concurrent).when(db).findByUuid(VM_UUID, KvmHypervisorInfoVO.class);
        doReturn(null).when(db).findByUuid(HOST_UUID, KvmHypervisorInfoVO.class);

        manager.saveNewHypervisorInfoList(Arrays.asList(concurrent, missing));

        verify(db).update(concurrent);
        verify(db).persist(missing);
    }

    @Test
    public void rethrowsPersistenceFailureNotCausedByIntegrityViolation() {
        KvmHypervisorInfoVO info = hypervisorInfo();
        PersistenceException failure = new PersistenceException("database connection is broken");
        doThrow(failure).when(db).persistCollection(anyCollection());

        try {
            manager.saveNewHypervisorInfoList(Collections.singletonList(info));
            Assert.fail("persistence failure not caused by integrity violation must be rethrown");
        } catch (PersistenceException e) {
            Assert.assertSame(failure, e);
        }

        verify(db, never()).updateCollection(anyCollection());
    }

    private KvmHypervisorInfoVO hypervisorInfo() {
        return hypervisorInfo(VM_UUID);
    }

    private KvmHypervisorInfoVO hypervisorInfo(String uuid) {
        KvmHypervisorInfoVO info = new KvmHypervisorInfoVO();
        info.setUuid(uuid);
        info.setHypervisor("qemu-kvm");
        info.setVersion("6.2.0-575.g0f7879b080.el8");
        return info;
    }

    private DataIntegrityViolationException duplicateEntry() {
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException(
                        String.format("Duplicate entry '%s' for key 'PRIMARY'", VM_UUID),
                        (SQLException) null, "PRIMARY"));
    }
}
