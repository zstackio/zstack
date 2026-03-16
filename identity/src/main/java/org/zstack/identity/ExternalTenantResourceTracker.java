package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.HardDeleteEntityExtensionPoint;
import org.zstack.core.db.SoftDeleteEntityByEOExtensionPoint;
import org.zstack.header.Component;
import org.zstack.header.aspect.OwnedByAccountAspectHelper;
import org.zstack.header.identity.*;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.vo.ResourceVO;
import org.apache.logging.log4j.ThreadContext;

import javax.persistence.EntityManager;
import java.util.*;

public class ExternalTenantResourceTracker implements
        HardDeleteEntityExtensionPoint,
        SoftDeleteEntityByEOExtensionPoint,
        Component,
        OwnedByAccountAspectHelper.ResourceOwnershipCreationNotifier {

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;

    private Map<String, ExternalTenantProvider> providers = new HashMap<>();

    @Override
    public boolean start() {
        for (ExternalTenantProvider p : pluginRgty.getExtensionList(ExternalTenantProvider.class)) {
            providers.put(p.getSource(), p);
        }

        // Populate AOP-level notifiers from PluginRegistry (standard extension point pattern).
        // AOP aspects cannot participate in Spring DI, so OwnedByAccountAspectHelper uses a
        // static field populated here during Component.start().
        OwnedByAccountAspectHelper.setResourceOwnershipCreationNotifiers(
                pluginRgty.getExtensionList(OwnedByAccountAspectHelper.ResourceOwnershipCreationNotifier.class));

        // Install interceptor to propagate ExternalTenantContext across CloudBus message
        // delivery threads. The strategy uses Log4j ThreadContext (MDC) as the transport
        // because CloudBus already propagates MDC via message headers (thread-context):
        //   send side: evalThreadContextToMessage() copies MDC → headers
        //   recv side: restores headers → MDC on the new worker thread
        //
        // For APIMessage: extract tenant context from session → set ThreadLocal + MDC
        // For non-APIMessage: restore tenant context from MDC → set ThreadLocal
        // This covers the full chain: APICreateXxxMsg → CreateXxxMsg → persist → AOP
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            private static final String MDC_TENANT_SOURCE = "x-tenant-source";
            private static final String MDC_TENANT_ID = "x-tenant-id";
            private static final String MDC_TENANT_USER = "x-tenant-user";

            @Override
            public void beforeDeliveryMessage(Message msg) {
                if (msg instanceof APIMessage) {
                    SessionInventory session = ((APIMessage) msg).getSession();
                    if (session != null && session.hasExternalTenant()) {
                        ExternalTenantContext ctx = session.getExternalTenantContext();
                        ExternalTenantContext.setCurrent(ctx);
                        // Write to MDC so CloudBus propagates to downstream internal messages
                        ThreadContext.put(MDC_TENANT_SOURCE, ctx.getSource());
                        ThreadContext.put(MDC_TENANT_ID, ctx.getTenantId());
                        if (ctx.getUserId() != null) {
                            ThreadContext.put(MDC_TENANT_USER, ctx.getUserId());
                        }
                    } else {
                        ExternalTenantContext.clearCurrent();
                        ThreadContext.remove(MDC_TENANT_SOURCE);
                        ThreadContext.remove(MDC_TENANT_ID);
                        ThreadContext.remove(MDC_TENANT_USER);
                    }
                } else {
                    // Non-APIMessage: restore tenant context from MDC (propagated via headers)
                    String source = ThreadContext.get(MDC_TENANT_SOURCE);
                    String tenantId = ThreadContext.get(MDC_TENANT_ID);
                    if (source != null && tenantId != null) {
                        String userId = ThreadContext.get(MDC_TENANT_USER);
                        ExternalTenantContext.setCurrent(
                                new ExternalTenantContext(source, tenantId, userId));
                    } else {
                        ExternalTenantContext.clearCurrent();
                    }
                }
            }
        });

        // Install a BeforeSendMessageInterceptor to ensure MDC has tenant keys
        // right before evalThreadContextToMessage() copies MDC → message headers.
        //
        // Why this is needed: ApiMediatorImpl.asyncCallMessageHandle() uses
        // thdf.syncSubmit() which runs dispatchMessage() on a DIFFERENT thread
        // from the one where BeforeDeliveryMessageInterceptor set the MDC.
        // The syncSubmit thread has clean MDC, so evalThreadContextToMessage()
        // copies empty tenant context into the message headers.
        //
        // This interceptor runs in doSendAndCallExtensions() right before doSend(),
        // on whatever thread calls bus.route(msg) / bus.send(msg). For APIMessage,
        // it reads tenant context from the session and writes MDC keys, ensuring
        // evalThreadContextToMessage() captures them regardless of thread.
        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            private static final String MDC_TENANT_SOURCE = "x-tenant-source";
            private static final String MDC_TENANT_ID = "x-tenant-id";
            private static final String MDC_TENANT_USER = "x-tenant-user";

            @Override
            public void beforeSendMessage(Message msg) {
                if (!(msg instanceof APIMessage)) {
                    return;
                }

                SessionInventory session = ((APIMessage) msg).getSession();
                if (session != null && session.hasExternalTenant()) {
                    ExternalTenantContext ctx = session.getExternalTenantContext();
                    ThreadContext.put(MDC_TENANT_SOURCE, ctx.getSource());
                    ThreadContext.put(MDC_TENANT_ID, ctx.getTenantId());
                    if (ctx.getUserId() != null) {
                        ThreadContext.put(MDC_TENANT_USER, ctx.getUserId());
                    }
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        OwnedByAccountAspectHelper.setResourceOwnershipCreationNotifiers(null);
        return true;
    }

    // --- AOP-level resource creation notification ---
    // ThreadLocal is propagated from APIMessage session via the BeforeDeliveryMessageInterceptor
    // installed in start(). For non-API paths (e.g. thdf.syncSubmit/chainSubmit), ThreadLocal
    // will not be set — those paths are not expected to carry tenant context in this MR.
    @Override
    public void notifyResourceOwnershipCreated(AccountResourceRefVO ref, EntityManager entityManager) {
        // AOP level cannot obtain session through method parameters, read from ThreadLocal
        ExternalTenantContext ctx = ExternalTenantContext.getCurrent();
        if (ctx == null || ctx.getTenantId() == null) {
            return;
        }

        ExternalTenantProvider provider = providers.get(ctx.getSource());
        if (provider == null) {
            return;
        }

        if (!provider.shouldTrackResource(ref.getResourceType())) {
            return;
        }

        ExternalTenantResourceRefVO extRef = new ExternalTenantResourceRefVO();
        extRef.setSource(ctx.getSource());
        extRef.setTenantId(ctx.getTenantId());
        extRef.setUserId(ctx.getUserId());
        extRef.setResourceUuid(ref.getResourceUuid());
        extRef.setResourceType(ref.getResourceType());
        extRef.setAccountUuid(ref.getAccountUuid());
        // Use the same EntityManager from the AOP aspect to stay within the same
        // flush/TX context. This avoids nested @DeadlockAutoRestart scopes from
        // dbf.persist() that cause unretryable transaction rollbacks under lock contention.
        entityManager.persist(extRef);

        provider.onResourceBound(ctx, ref.getResourceUuid(), ref.getResourceType());
    }

    // --- Resource deletion cleanup ---
    // Note: ExternalTenantResourceRefVO has FK CASCADE on resourceUuid → ResourceVO.uuid,
    // so DB-level cascade already handles cleanup on hard delete. These extension points
    // serve as defense-in-depth for cases where the delete path bypasses FK constraints
    // (e.g. batch SQL delete without loading entities).
    @Override
    public List<Class> getEntityClassForHardDeleteEntityExtension() {
        return Collections.singletonList(ResourceVO.class);
    }

    @Override
    public void postHardDelete(Collection entityIds, Class entityClass) {
        cleanupTenantRefs(entityIds);
    }

    // Soft delete cleanup: returns ResourceVO.class as the EO class.
    // ZStack's SoftDeleteEntityByEOExtensionPoint uses isAssignableFrom matching,
    // so registering the base ResourceVO.class catches all concrete EO subclasses
    // (e.g. VmInstanceEO, VolumeEO). If the framework ever changes to exact-match,
    // this would stop firing — but FK CASCADE on hard delete (expunge) ensures
    // no orphan ExternalTenantResourceRefVO records remain.
    @Override
    public List<Class> getEOClassForSoftDeleteEntityExtension() {
        return Collections.singletonList(ResourceVO.class);
    }

    @Override
    public void postSoftDelete(Collection entityIds, Class EOClass) {
        cleanupTenantRefs(entityIds);
    }

    private void cleanupTenantRefs(Collection entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return;
        }

        new SQLBatch() {
            @Override
            protected void scripts() {
                sql("DELETE FROM ExternalTenantResourceRefVO WHERE resourceUuid IN (:uuids)")
                    .param("uuids", entityIds)
                    .execute();
            }
        }.execute();
    }
}
