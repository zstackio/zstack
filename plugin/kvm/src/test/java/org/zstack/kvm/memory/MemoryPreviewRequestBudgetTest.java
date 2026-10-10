package org.zstack.kvm.memory;

import com.google.gson.GsonBuilder;
import org.junit.Test;
import org.zstack.header.message.APIParam;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class MemoryPreviewRequestBudgetTest {
    private APIPreviewMemoryPolicyMsg message() {
        APIPreviewMemoryPolicyMsg msg = APIPreviewMemoryPolicyMsg.__example__();
        msg.setAction("clearOverride");
        msg.setPolicy("{}");
        msg.setClearOverrideFields(Arrays.asList("ksm.enabled", "zram.enabled"));
        return msg;
    }

    private Map<String, Object> envelope(APIPreviewMemoryPolicyMsg msg) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", msg.getScope());
        result.put("resourceUuid", msg.getResourceUuid());
        result.put("policy", msg.getPolicy());
        result.put("targetHostUuids", msg.getTargetHostUuids());
        result.put("action", msg.getAction());
        result.put("clearOverrideFields", msg.getClearOverrideFields());
        result.values().removeAll(Collections.singleton(null));
        return result;
    }

    private long size(APIPreviewMemoryPolicyMsg msg) {
        return new GsonBuilder().disableHtmlEscaping().create().toJson(envelope(msg))
                .getBytes(StandardCharsets.UTF_8).length;
    }

    private void rejected(APIPreviewMemoryPolicyMsg msg, long budget) {
        try {
            MemoryApiRequestBudget.validatePreview(msg, budget);
            fail("expected the complete preview envelope to exceed its budget");
        } catch (MemoryOperationException e) {
            assertEquals("MEMORY_REQUEST_TOO_LARGE", e.getCode());
        }
    }

    @Test
    public void longClearFieldListCannotEscapeBudget() {
        APIPreviewMemoryPolicyMsg msg = message();
        msg.setClearOverrideFields(Collections.nCopies(128, "zram.enabled"));
        rejected(msg, 1024);
    }

    @Test
    public void exactCompleteEnvelopeBoundaryIncludesActionAndClearFields() {
        APIPreviewMemoryPolicyMsg msg = message();
        long bytes = size(msg);
        MemoryApiRequestBudget.validatePreview(msg, bytes);
        rejected(msg, bytes - 1);
    }

    @Test
    public void targetListAndUtf8UseBytesRatherThanCharacterCount() {
        APIPreviewMemoryPolicyMsg msg = message();
        msg.setScope("Global");
        msg.setResourceUuid("global");
        msg.setTargetHostUuids(Arrays.asList("0123456789abcdef0123456789abcdef"));
        // Request budget admission precedes business policy validation.
        msg.setPolicy("{\"description\":\"内存\"}");
        long bytes = size(msg);
        MemoryApiRequestBudget.validatePreview(msg, bytes);
        rejected(msg, bytes - 1);
    }

    @Test
    public void defaultApplyActionIsCountedAndNullOptionalFieldsAreOmitted() {
        APIPreviewMemoryPolicyMsg msg = APIPreviewMemoryPolicyMsg.__example__();
        msg.setAction(null);
        long bytes = size(msg);
        MemoryApiRequestBudget.validatePreview(msg, bytes);
        rejected(msg, bytes - 1);
    }

    @Test
    public void ordinaryClearPreviewFitsDefaultBudgetButZeroBudgetIsRejected() {
        APIPreviewMemoryPolicyMsg msg = message();
        MemoryApiRequestBudget.validatePreview(msg, MemoryApiRequestBudget.DEFAULT_BYTES);
        rejected(msg, 0);
    }

    @Test
    public void envelopeCoversEveryDeclaredBusinessParameter() {
        Set<String> fields = new LinkedHashSet<>();
        for (Field field : APIPreviewMemoryPolicyMsg.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(APIParam.class)) {
                fields.add(field.getName());
            }
        }
        assertEquals(new LinkedHashSet<>(Arrays.asList("scope", "resourceUuid", "policy",
                "targetHostUuids", "action", "clearOverrideFields")), fields);
    }
}
