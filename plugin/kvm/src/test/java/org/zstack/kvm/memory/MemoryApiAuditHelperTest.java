package org.zstack.kvm.memory;

import org.junit.Test;
import org.junit.Assume;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vo.ResourceVO;

import java.util.Arrays;
import java.util.List;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class MemoryApiAuditHelperTest {
    private static final String RESOURCE = "0123456789abcdef0123456789abcdef";
    private static final String TASK = "abcdef0123456789abcdef0123456789";

    private APIUpdateMemoryPolicyMsg update(String scope, String resource) {
        APIUpdateMemoryPolicyMsg msg = new APIUpdateMemoryPolicyMsg();
        msg.setScope(scope); msg.setResourceUuid(resource); msg.setAction("apply");
        return msg;
    }

    private APIUpdateMemoryPolicyEvent acceptedTask(String scope, String resource, String action,
                                                     String status) {
        APIUpdateMemoryPolicyEvent event = new APIUpdateMemoryPolicyEvent("api-id");
        MemoryTaskInventory task = new MemoryTaskInventory();
        task.setUuid(TASK); task.setScope(scope); task.setResourceUuid(resource);
        task.setAction(action); task.setStatus(status);
        event.setInventory(task);
        return event;
    }

    private void assertResource(List<APIAuditor.Result> results, int index, String uuid, Class<?> type) {
        assertEquals(uuid, results.get(index).getResourceUuid());
        assertEquals(type, results.get(index).getResourceType());
    }

    @Test public void directScopeMapsToTheCorrespondingRealResourceTypeAfterAcceptedApi() {
        APIEvent accepted = new APIEvent("api-id");
        assertResource(MemoryApiAuditHelper.update(update("Host", RESOURCE), accepted), 0,
                RESOURCE, HostVO.class);
        assertResource(MemoryApiAuditHelper.update(update("Cluster", RESOURCE), accepted), 0,
                RESOURCE, ClusterVO.class);
        assertResource(MemoryApiAuditHelper.update(update("VM", RESOURCE), accepted), 0,
                RESOURCE, VmInstanceVO.class);
    }

    @Test public void rejectedDirectUpdateKeepsAuditEvidenceWithoutCrossAccountAssociation() {
        APIEvent rejected = new APIEvent("api-id"); rejected.setSuccess(false);
        List<APIAuditor.Result> result = MemoryApiAuditHelper.update(update("Host", RESOURCE), rejected);
        assertEquals(1, result.size());
        assertResource(result, 0, "", ResourceVO.class);
    }

    @Test public void invalidScopeIsRetainedWithoutFalselyAssociatingItToHost() {
        List<APIAuditor.Result> result = MemoryApiAuditHelper.update(update("Other", RESOURCE),
                new APIEvent("api-id"));
        assertEquals(1, result.size());
        assertEquals("", result.get(0).getResourceUuid());
        assertEquals(ResourceVO.class, result.get(0).getResourceType());
    }

    @Test public void globalTargetsRequireSuccessfulMatchingAcceptedTaskAndKeepTaskTrace() {
        APIUpdateMemoryPolicyMsg msg = update("Global", "global");
        msg.setTargetHostUuids(Arrays.asList(RESOURCE, TASK));
        APIUpdateMemoryPolicyEvent response = acceptedTask("Global", "global", "apply", "Queued");
        List<APIAuditor.Result> results = MemoryApiAuditHelper.update(msg, response);
        assertEquals(2, results.size());
        assertResource(results, 0, RESOURCE, HostVO.class);
        assertResource(results, 1, TASK, HostVO.class);
        // The audit helper associates accepted work; it never rewrites an
        // asynchronous Queued task into an execution-success status.
        assertEquals("Queued", response.getInventory().getStatus());

        APIUpdateMemoryPolicyEvent failed = acceptedTask("Global", "global", "apply", "Queued");
        failed.setSuccess(false);
        List<APIAuditor.Result> rejected = MemoryApiAuditHelper.update(msg, failed);
        assertEquals(1, rejected.size());
        assertEquals("", rejected.get(0).getResourceUuid());
        assertEquals(ResourceVO.class, rejected.get(0).getResourceType());
    }

    @Test public void globalShardTargetsAreNotTrustedResourceAssociations() {
        APIUpdateMemoryPolicyMsg msg = update("Global", "global");
        msg.setAction("stageTargetShard");
        msg.setTargetHostUuids(Arrays.asList(RESOURCE));
        msg.setTargetVmUuids(Arrays.asList(TASK));
        List<APIAuditor.Result> mixed = MemoryApiAuditHelper.update(msg,
                acceptedTask("Global", "global", "stageTargetShard", "Staged"));
        assertEquals(1, mixed.size());
        assertEquals(ResourceVO.class, mixed.get(0).getResourceType());

        msg.setTargetVmUuids(null);
        msg.setAction("apply");
        msg.setTargetHostUuids(Arrays.asList(RESOURCE, "not-a-uuid"));
        List<APIAuditor.Result> malformed = MemoryApiAuditHelper.update(msg,
                acceptedTask("Global", "global", "apply", "Draining"));
        assertEquals(1, malformed.size());
        assertEquals(ResourceVO.class, malformed.get(0).getResourceType());
    }

    @Test public void cancellationAssociatesUnderlyingScopeButNeverTreatsTaskAsHost() {
        APICancelMemoryTaskMsg msg = new APICancelMemoryTaskMsg(); msg.setUuid(TASK);
        APICancelMemoryTaskEvent response = new APICancelMemoryTaskEvent("api-id");
        MemoryTaskInventory task = new MemoryTaskInventory();
        task.setUuid(TASK); task.setScope("Host"); task.setResourceUuid(RESOURCE);
        task.setStatus("Cancelled"); response.setInventory(task);
        List<APIAuditor.Result> results = MemoryApiAuditHelper.cancel(msg, response);
        assertEquals(1, results.size());
        assertResource(results, 0, RESOURCE, HostVO.class);

        APICancelMemoryTaskEvent rejected = new APICancelMemoryTaskEvent("api-id");
        rejected.setSuccess(false);
        List<APIAuditor.Result> failed = MemoryApiAuditHelper.cancel(msg, rejected);
        assertEquals(1, failed.size());
        assertResource(failed, 0, "", ResourceVO.class);
    }

    @Test public void cancelWithoutTaskUuidStillProducesUnassociatedFailureEvidence() {
        APICancelMemoryTaskMsg msg = new APICancelMemoryTaskMsg();
        APICancelMemoryTaskEvent rejected = new APICancelMemoryTaskEvent("api-id");
        rejected.setSuccess(false);
        List<APIAuditor.Result> result = MemoryApiAuditHelper.cancel(msg, rejected);
        assertEquals(1, result.size());
        assertEquals("", result.get(0).getResourceUuid());
        assertEquals(ResourceVO.class, result.get(0).getResourceType());
    }

    @Test public void deleteAuditUsesOnlyMatchingLockedTaskResourceLinks() {
        APIDeleteMemoryTaskMsg msg = new APIDeleteMemoryTaskMsg(); msg.setUuid(TASK);
        APIDeleteMemoryTaskEvent response = new APIDeleteMemoryTaskEvent("api-id");
        response.setTaskUuid(TASK); response.setDeleted(true); response.setScope("Global");
        response.setResourceUuid("global"); response.setHostUuids(Arrays.asList(RESOURCE));
        List<APIAuditor.Result> results = MemoryApiAuditHelper.delete(msg, response);
        assertEquals(1, results.size());
        assertResource(results, 0, RESOURCE, HostVO.class);

        // The global-task host list is an all-or-nothing association: malformed
        // or duplicate response links must not leave a partial resource audit.
        response.setHostUuids(Arrays.asList(RESOURCE, "bad-uuid"));
        List<APIAuditor.Result> malformedLinks = MemoryApiAuditHelper.delete(msg, response);
        assertEquals(1, malformedLinks.size());
        assertResource(malformedLinks, 0, "", ResourceVO.class);

        response.setHostUuids(Arrays.asList(RESOURCE, RESOURCE));
        List<APIAuditor.Result> duplicateLinks = MemoryApiAuditHelper.delete(msg, response);
        assertEquals(1, duplicateLinks.size());
        assertResource(duplicateLinks, 0, "", ResourceVO.class);
        response.setHostUuids(Arrays.asList(RESOURCE));

        response.setTaskUuid(RESOURCE);
        List<APIAuditor.Result> mismatched = MemoryApiAuditHelper.delete(msg, response);
        assertEquals(1, mismatched.size());
        assertResource(mismatched, 0, "", ResourceVO.class);
        response.setTaskUuid(TASK); response.setDeleted(false);
        List<APIAuditor.Result> missing = MemoryApiAuditHelper.delete(msg, response);
        assertEquals(1, missing.size());
        assertResource(missing, 0, "", ResourceVO.class);
    }

    @Test public void mismatchedCancelResponseCannotRedirectTheAuditAssociation() {
        APICancelMemoryTaskMsg msg = new APICancelMemoryTaskMsg(); msg.setUuid(TASK);
        APICancelMemoryTaskEvent response = new APICancelMemoryTaskEvent("api-id");
        MemoryTaskInventory other = new MemoryTaskInventory();
        other.setUuid(RESOURCE); other.setScope("Host"); other.setResourceUuid("fedcba9876543210fedcba9876543210");
        response.setInventory(other);
        List<APIAuditor.Result> result = MemoryApiAuditHelper.cancel(msg, response);
        assertEquals(1, result.size());
        assertResource(result, 0, "", ResourceVO.class);
    }

    @Test public void taskEntityIsNotAPlatformResourceType() {
        assertFalse(ResourceVO.class.isAssignableFrom(MemoryTaskVO.class));
        assertTrue(ResourceVO.class.isAssignableFrom(HostVO.class));
        assertTrue(ResourceVO.class.isAssignableFrom(ClusterVO.class));
        assertTrue(ResourceVO.class.isAssignableFrom(VmInstanceVO.class));
    }

    @Test public void standardMultiAuditorReflectionFindsAndInvokesTheNewAuditResult() throws Exception {
        assertTrue(APIMultiAuditor.class.isAssignableFrom(APIUpdateMemoryPolicyMsg.class));
        assertTrue(APIMultiAuditor.class.isAssignableFrom(APICancelMemoryTaskMsg.class));
        assertTrue(APIMultiAuditor.class.isAssignableFrom(APIDeleteMemoryTaskMsg.class));
        Method multiAudit = APIUpdateMemoryPolicyMsg.class.getMethod("multiAudit",
                org.zstack.header.message.APIMessage.class,
                org.zstack.header.message.APIEvent.class);
        APIUpdateMemoryPolicyMsg msg = update("Host", RESOURCE);
        List<?> linked = (List<?>) multiAudit.invoke(msg, msg,
                new APIUpdateMemoryPolicyEvent("api-id"));
        assertEquals(1, linked.size());
        APIAuditor.Result result = (APIAuditor.Result) linked.get(0);
        assertEquals(RESOURCE, result.getResourceUuid());
        assertEquals(HostVO.class, result.getResourceType());
    }

    @Test public void frozenLegacyWarMessagesFailTheResourceAuditCapabilityCheck() throws Exception {
        String jar = System.getProperty("f05.legacy.kvm.jar",
                "/dev/shm/r13-revision-proof-r1-legacy-war-probe.json/libs/kvm-5.5.0.jar");
        Assume.assumeTrue("set f05.legacy.kvm.jar to the frozen legacy WAR kvm jar", new File(jar).isFile());
        try (URLClassLoader legacy = new ChildFirstApiLoader(new URL[]{new File(jar).toURI().toURL()},
                getClass().getClassLoader())) {
            Class<?> oldUpdate = Class.forName("org.zstack.kvm.memory.APIUpdateMemoryPolicyMsg", true, legacy);
            Class<?> oldCancel = Class.forName("org.zstack.kvm.memory.APICancelMemoryTaskMsg", true, legacy);
            assertFalse(APIMultiAuditor.class.isAssignableFrom(oldUpdate));
            assertFalse(APIMultiAuditor.class.isAssignableFrom(oldCancel));
            assertNoTypedResourceUuid(oldUpdate);
            assertNoTypedResourceUuid(oldCancel);

            // Exercise the same required capability assertion used by the
            // standard reflection collector. It must fail on frozen classes.
            boolean oldContractWouldFail = false;
            try {
                assertTrue("legacy API has a standard resource audit resolver",
                        APIMultiAuditor.class.isAssignableFrom(oldUpdate));
            } catch (AssertionError expectedRed) {
                oldContractWouldFail = true;
            }
            assertTrue("frozen WAR must reproduce the pre-fix audit-link failure", oldContractWouldFail);
            assertTrue(APIMultiAuditor.class.isAssignableFrom(APIUpdateMemoryPolicyMsg.class));
            assertTrue(APIMultiAuditor.class.isAssignableFrom(APICancelMemoryTaskMsg.class));
        }
    }

    private void assertNoTypedResourceUuid(Class<?> apiClass) {
        for (java.lang.reflect.Field field : apiClass.getDeclaredFields()) {
            org.zstack.header.message.APIParam annotation = field.getAnnotation(
                    org.zstack.header.message.APIParam.class);
            if (annotation != null && annotation.resourceType() != Object.class) {
                fail("legacy message unexpectedly carries a typed resource UUID field: " + field.getName());
            }
        }
    }

    private static class ChildFirstApiLoader extends URLClassLoader {
        private static final List<String> TARGETS = Arrays.asList(
                "org.zstack.kvm.memory.APIUpdateMemoryPolicyMsg",
                "org.zstack.kvm.memory.APICancelMemoryTaskMsg");
        ChildFirstApiLoader(URL[] urls, ClassLoader parent) { super(urls, parent); }
        @Override protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (TARGETS.contains(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) { loaded = findClass(name); }
                if (resolve) { resolveClass(loaded); }
                return loaded;
            }
            return super.loadClass(name, resolve);
        }
    }
}
