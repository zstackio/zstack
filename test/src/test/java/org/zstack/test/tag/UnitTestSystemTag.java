package org.zstack.test.tag;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.tag.SystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManagerImpl;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by lining on 02/25/17.
 */
public class UnitTestSystemTag {
    @InjectMocks
    SystemTag systemTag;

    @Mock
    DatabaseFacade dbf;

    @Mock
    TagManagerImpl tagMgr;

    @Before
    public void setUp() {
        systemTag = new SystemTag("capability::liveSnapshot", HostVO.class);
        MockitoAnnotations.initMocks(this);
    }

    public void mockCreate(String resUuid){
        EntityManager entityManager = Mockito.mock(EntityManager.class);
        Mockito.when(dbf.getEntityManager()).thenReturn(entityManager);

        TypedQuery<Long> typedQuery = Mockito.mock(TypedQuery.class);

        Mockito.when(entityManager.createQuery("select count(t) from SystemTagVO t where t.uuid = :uuid", Long.class)).thenAnswer(new Answer<TypedQuery<Long>>() {
            @Override
            public TypedQuery<Long> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return typedQuery;
            }
        });

        String key = String.format("%s-%s-%s", resUuid, systemTag.getResourceClass().getSimpleName(), systemTag.getTagFormat());
        String uuid = UUID.nameUUIDFromBytes(key.getBytes()).toString().replaceAll("-", "");
        List<SystemTagVO> list = new ArrayList<SystemTagVO>();
        SystemTagVO systemTagVO = new SystemTagVO();
        systemTagVO.setUuid(uuid);
        list.add(systemTagVO);
        Mockito.when(typedQuery.getSingleResult()).thenReturn(1L);
    }

    /**
     * Test method for {@link SystemTag#newSystemTagCreator(String)}
     *
     * case: test create duplicate systemTag
     *
     * condition:
     * ignoreIfExisting = false; unique = true; ignoreIfExisting = true
     *
     * expect: return null
     */
    @Test
    public void testNewSystemTagCreator_0(){

        String resUuid = UUID.randomUUID().toString();
        SystemTagCreator systemTagCreator = systemTag.newSystemTagCreator(resUuid);
        systemTagCreator.recreate = false;
        systemTagCreator.unique = true;
        systemTagCreator.ignoreIfExisting = true;

        mockCreate(resUuid);

        SystemTagInventory result = systemTagCreator.create();
        Assert.assertNull(result);
    }

    /**
     * Test method for {@link SystemTag#newSystemTagCreator(String)}
     *
     * case: test create duplicate systemTag
     *
     * condition:
     * ignoreIfExisting = false; unique = true; ignoreIfExisting = false
     *
     * expect: throw exception
     */
    @Test(expected = CloudRuntimeException.class)
    public void testNewSystemTagCreator_1(){

        String resUuid = UUID.randomUUID().toString();
        SystemTagCreator systemTagCreator = systemTag.newSystemTagCreator(resUuid);
        systemTagCreator.recreate = false;
        systemTagCreator.unique = true;
        systemTagCreator.ignoreIfExisting = false;

        mockCreate(resUuid);

        systemTagCreator.create();
    }

}