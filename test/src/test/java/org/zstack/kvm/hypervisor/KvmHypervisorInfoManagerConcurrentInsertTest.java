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
import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 一个 host/vm uuid 的 hypervisor 信息可能被多条异步路径并发上报（云主机启动时 StartVm 响应与
 * libvirtReportStart 事件），先查后写会同时判定成「行不存在」，后提交的事务会违反主键。
 */
public class KvmHypervisorInfoManagerConcurrentInsertTest {
    private static final String VM_UUID = "4a6173618107489e8013f294e37a533d";

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
    public void fallsBackToUpdateWhenAnotherTransactionInsertedTheSameRow() {
        KvmHypervisorInfoVO info = hypervisorInfo();
        doThrow(duplicateEntry()).when(db).persistCollection(anyCollection());

        manager.saveNewHypervisorInfoList(Collections.singletonList(info));

        verify(db).updateCollection(Collections.singletonList(info));
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
        KvmHypervisorInfoVO info = new KvmHypervisorInfoVO();
        info.setUuid(VM_UUID);
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
