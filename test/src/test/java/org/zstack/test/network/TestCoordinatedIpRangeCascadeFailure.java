package org.zstack.test.network;

import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.network.l3.*;
import org.zstack.network.l3.IpRangeCascadeExtension;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TestCoordinatedIpRangeCascadeFailure {
    @Test
    public void coordinatedDeleteFailureReachesParent() throws Exception {
        assertDeletionOutcome(true, false);
    }

    @Test
    public void forceDoesNotHideCoordinatedDeleteFailure() throws Exception {
        assertDeletionOutcome(true, true);
    }

    @Test
    public void legacyDeleteKeepsExistingBestEffortBehavior() throws Exception {
        assertDeletionOutcome(false, false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertDeletionOutcome(boolean coordinated, boolean force) throws Exception {
        DatabaseFacade dbf = mock(DatabaseFacade.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        CriteriaQuery<Long> countCriteria = mock(CriteriaQuery.class, RETURNS_SELF);
        CriteriaQuery<String> l2Criteria = mock(CriteriaQuery.class, RETURNS_SELF);
        when(countCriteria.from(AddressPoolVO.class)).thenReturn(mock(Root.class));
        when(l2Criteria.from(L3NetworkVO.class)).thenReturn(mock(Root.class));
        when(builder.createQuery(Long.class)).thenReturn(countCriteria);
        when(builder.createQuery(String.class)).thenReturn(l2Criteria);
        when(dbf.getCriteriaBuilder()).thenReturn(builder);
        EntityManager em = mock(EntityManager.class);
        when(dbf.getEntityManager()).thenReturn(em);
        TypedQuery<Long> countQuery = mock(TypedQuery.class);
        TypedQuery<String> l2Query = mock(TypedQuery.class);
        when(em.createQuery(countCriteria)).thenReturn(countQuery);
        when(em.createQuery(l2Criteria)).thenReturn(l2Query);
        when(countQuery.getSingleResult()).thenReturn(0L);
        when(l2Query.getSingleResult()).thenReturn("l2");

        ComponentLoader loader = mock(ComponentLoader.class);
        when(loader.getComponent(DatabaseFacade.class)).thenReturn(dbf);
        Field loaderField = Platform.class.getDeclaredField("loader");
        loaderField.setAccessible(true);
        Object previousLoader = loaderField.get(null);
        SingularAttribute previousL2Attribute = L3NetworkVO_.l2NetworkUuid;
        SingularAttribute l2Attribute = mock(SingularAttribute.class);
        when(l2Attribute.getJavaType()).thenReturn(String.class);
        L3NetworkVO_.l2NetworkUuid = l2Attribute;
        loaderField.set(null, loader);
        try {
            IpRangeCascadeExtension extension = new IpRangeCascadeExtension();
            CloudBus bus = mock(CloudBus.class);
            Field busField = IpRangeCascadeExtension.class.getDeclaredField("bus");
            busField.setAccessible(true);
            busField.set(extension, bus);
            IpRangeDeletionReply reply = JSONObjectUtil.toObject(
                    "{\"coordinatedNetworkConfigFailure\":" + coordinated + "}", IpRangeDeletionReply.class);
            ErrorCode childError = new ErrorCode();
            childError.setCode("TEST.IPAM.DELETE.REJECTED");
            childError.setDetails("remote IPAM still has a port allocation");
            reply.setError(childError);
            doAnswer(call -> {
                CloudBusCallBack callback = call.getArgument(1);
                callback.run(reply);
                return null;
            }).when(bus).send(any(IpRangeDeletionMsg.class), any(CloudBusCallBack.class));

            IpRangeInventory range = new IpRangeInventory();
            range.setUuid("range");
            range.setL3NetworkUuid("l3");
            CascadeAction action = new CascadeAction()
                    .setActionCode(force ? CascadeConstant.DELETION_FORCE_DELETE_CODE
                            : CascadeConstant.DELETION_DELETE_CODE)
                    .setParentIssuer(IpRangeVO.class.getSimpleName())
                    .setParentIssuerContext(Collections.singletonList(range));
            AtomicInteger success = new AtomicInteger();
            AtomicInteger failed = new AtomicInteger();
            AtomicReference<ErrorCode> actualError = new AtomicReference<>();
            extension.asyncCascade(action, new Completion(null) {
                @Override
                public void success() { success.incrementAndGet(); }

                @Override
                public void fail(ErrorCode error) {
                    failed.incrementAndGet();
                    actualError.set(error);
                }
            });
            verify(bus, times(1)).send(any(IpRangeDeletionMsg.class), any(CloudBusCallBack.class));
            assertEquals("cascade must complete exactly once", 1, success.get() + failed.get());
            assertEquals("coordinated remote deletion failure must reach parent", coordinated ? 1 : 0, failed.get());
            if (coordinated) {
                assertNotNull(actualError.get());
                assertTrue("child rejection must survive propagation", JSONObjectUtil.toJsonString(actualError.get())
                        .contains(childError.getCode()));
            }
        } finally {
            loaderField.set(null, previousLoader);
            L3NetworkVO_.l2NetworkUuid = previousL2Attribute;
        }
    }
}
