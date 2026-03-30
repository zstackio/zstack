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

    /**
     * Dedicated message header key for propagating ExternalTenantContext across
     * CloudBus message delivery boundaries. This replaces the previous MDC-based
     * approach which was susceptible to thread-pool reuse causing context leaks
     * between concurrent requests.
     *
     * The header carries a String[] {source, tenantId, userId} and is set by
     * BeforeSendMessageInterceptor on the send side, read by
     * BeforeDeliveryMessageInterceptor on the receive side.
     */
    private static final String HEADER_EXTERNAL_TENANT = "external-tenant-context";

    @Override
    public boolean start() {
        for (ExternalTenantProvider p : pluginRgty.getExtensionList(ExternalTenantProvider.class)) {
            providers.put(p.getSource(), p);
        }

        OwnedByAccountAspectHelper.setResourceOwnershipCreationNotifiers(
                pluginRgty.getExtensionList(OwnedByAccountAspectHelper.ResourceOwnershipCreationNotifier.class));

        // === Send-side interceptor ===
        // Before each message is sent via CloudBus, copy the current thread's
        // ExternalTenantContext (ThreadLocal) into a dedicated message header.
        // This makes tenant context message-scoped rather than thread-scoped,
        // eliminating race conditions from thread-pool reuse.
        //
        // Flow: ThreadLocal → msg.header["external-tenant-context"]
        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            public void beforeSendMessage(Message msg) {
                ExternalTenantContext ctx = ExternalTenantContext.getCurrent();
                if (ctx != null && ctx.getTenantId() != null) {
                    msg.putHeaderEntry(HEADER_EXTERNAL_TENANT,
                            new String[]{ctx.getSource(), ctx.getTenantId(), ctx.getUserId()});
                }
            }
        });

        // === Receive-side interceptor ===
        // On message delivery, restore ExternalTenantContext to ThreadLocal.
        //
        // For APIMessage: authoritative source is session.externalTenantContext
        //   (set by RestServer from HTTP headers). Write to ThreadLocal so AOP can read it.
        //
        // For non-APIMessage (internal messages like CreateVmInstanceMsg):
        //   read from the dedicated message header written by the send-side interceptor.
        //   This is the key fix — previously this read from MDC which is per-thread
        //   and gets corrupted under concurrent requests sharing the same thread pool.
        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void beforeDeliveryMessage(Message msg) {
                if (msg instanceof APIMessage) {
                    SessionInventory session = ((APIMessage) msg).getSession();
                    if (session != null && session.hasExternalTenant()) {
                        ExternalTenantContext ctx = session.getExternalTenantContext();
                        ExternalTenantContext.setCurrent(ctx);
                    } else {
                        ExternalTenantContext.clearCurrent();
                    }
                } else {
                    // Non-APIMessage: restore from dedicated message header (message-scoped)
                    String[] tenantData = msg.getHeaderEntry(HEADER_EXTERNAL_TENANT);
                    if (tenantData != null && tenantData.length >= 2) {
                        String userId = tenantData.length >= 3 ? tenantData[2] : null;
                        ExternalTenantContext.setCurrent(
                                new ExternalTenantContext(tenantData[0], tenantData[1], userId));
                    } else {
                        ExternalTenantContext.clearCurrent();
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

    @Override
    public void notifyResourceOwnershipCreated(AccountResourceRefVO ref, EntityManager entityManager) {
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
        entityManager.persist(extRef);

        provider.onResourceBound(ctx, ref.getResourceUuid(), ref.getResourceType());
    }

    @Override
    public List<Class> getEntityClassForHardDeleteEntityExtension() {
        return Collections.singletonList(ResourceVO.class);
    }

    @Override
    public void postHardDelete(Collection entityIds, Class entityClass) {
        cleanupTenantRefs(entityIds);
    }

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
