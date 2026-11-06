package org.zstack.test.longjob;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJob;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.APIEvent;
import org.apache.commons.beanutils.PropertyUtils;

import static org.junit.Assert.*;

public class TestLongJobInventorySecurity {
    @Test
    public void publicInventoryDoesNotExposeInternalSession() {
        String raw = "{\"session\":{\"uuid\":\"owner-test-session\"},\"resourceUuid\":\"resource-id\",\"cpuNum\":4}";
        LongJobVO vo = new LongJobVO();
        vo.setJobData(raw);
        LongJobInventory inv = LongJobInventory.valueOf(vo);
        assertFalse(inv.getJobData().contains("owner-test-session"));
        assertEquals(raw, vo.getJobData());
        JsonObject data = new JsonParser().parse(inv.getJobData()).getAsJsonObject();
        assertEquals("resource-id", data.get("resourceUuid").getAsString());
        assertEquals(4, data.get("cpuNum").getAsInt());
    }

    @Test
    public void projectedFieldsAndNestedSerializedMessagesAreSafe() throws Exception {
        String raw = "{\"items\":[{\"session\":{\"uuid\":\"nested-session\"}}],\"payload\":\"{\\\"password\\\":\\\"nested-password\\\",\\\"name\\\":\\\"model\\\"}\",\"credential\":{\"accessKey\":\"test-key\"}}";
        LongJobInventory inv = new LongJobInventory();
        PropertyUtils.setProperty(inv, "jobData", raw);
        assertFalse(inv.getJobData().contains("nested-session"));
        assertFalse(inv.getJobData().contains("nested-password"));
        assertFalse(inv.getJobData().contains("test-key"));
        assertTrue(inv.getJobData().contains("model"));
    }

    @Test
    public void publicMappingLeavesResumeCredentialsInTheInternalVo() {
        LongJobVO vo = new LongJobVO();
        vo.setJobData("{\"session\":{\"uuid\":\"resume-test-session\"},\"password\":\"resume-test-password\"}");
        LongJobInventory.valueOf(vo);
        LongJob handler = new LongJob() {
            @Override
            public void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
                fail("resume must use the resume handler");
            }

            @Override
            public void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {
                JsonObject data = new JsonParser().parse(job.getJobData()).getAsJsonObject();
                assertEquals("resume-test-session", data.getAsJsonObject("session").get("uuid").getAsString());
                assertEquals("resume-test-password", data.get("password").getAsString());
            }
        };
        handler.resume(vo, null);
    }

    @Test
    public void credentialSuffixesAreRemovedFromPublicMappingAndProjection() throws Exception {
        JsonObject credentials = new JsonObject();
        String[] fields = {"apiKey", "sshPrivateKey", "userAccessKey", "secretAccessKey",
                "API_KEY", "ssh-private-key", "USER_ACCESS_KEY"};
        for (String field : fields) {
            credentials.addProperty(field, "fixture-" + field);
        }
        JsonObject payload = new JsonObject();
        payload.add("nested", credentials);
        payload.addProperty("serialized", credentials.toString());
        String raw = payload.toString();
        LongJobVO vo = new LongJobVO();
        vo.setJobData(raw);
        LongJobInventory mapped = LongJobInventory.valueOf(vo);
        LongJobInventory projected = new LongJobInventory();
        PropertyUtils.setProperty(projected, "jobData", raw);
        for (String field : fields) {
            assertFalse(field, mapped.getJobData().contains("fixture-" + field));
            assertFalse(field, projected.getJobData().contains("fixture-" + field));
        }
        assertEquals(raw, vo.getJobData());
    }

    @Test
    public void nonCredentialKeysAndOrdinaryTextRemainVisible() {
        JsonObject data = new JsonObject();
        String[] fields = {"monkey", "hockey", "key", "publicKey", "sshPublicKey", "lookupKey"};
        for (String field : fields) {
            data.addProperty(field, "visible-" + field);
        }
        data.addProperty("message", "training [phase 1] {waiting}");
        LongJobInventory inv = new LongJobInventory();
        inv.setJobData(data.toString());
        JsonObject safe = new JsonParser().parse(inv.getJobData()).getAsJsonObject();
        for (String field : fields) {
            assertEquals("visible-" + field, safe.get(field).getAsString());
        }
        assertEquals("training [phase 1] {waiting}", safe.get("message").getAsString());
    }

    @Test
    public void malformedNestedJsonStringsFailClosed() {
        JsonObject data = new JsonObject();
        data.addProperty("message", "{not-json");
        data.addProperty("array", "[unterminated");
        data.addProperty("payload", "{\"apiKey\":\"fixture-malformed-credential\"");
        LongJobInventory inv = new LongJobInventory();
        inv.setJobData(data.toString());
        JsonObject safe = new JsonParser().parse(inv.getJobData()).getAsJsonObject();
        assertEquals("{}", safe.get("message").getAsString());
        assertEquals("{}", safe.get("array").getAsString());
        assertEquals("{}", safe.get("payload").getAsString());
        assertFalse(inv.getJobData().contains("fixture-malformed-credential"));
    }

    @Test
    public void defaultJUnitSuiteIncludesSecurityRegression() throws Exception {
        Class<?> suite = Class.forName("org.zstack.test.unittest.JUnitTestSuite");
        Class<?>[] cases = suite.getAnnotation(org.junit.runners.Suite.SuiteClasses.class).value();
        assertTrue("default JUnit entry must execute the public-data security contract",
                java.util.Arrays.asList(cases).contains(TestLongJobInventorySecurity.class));
    }

    @Test
    public void nullAndUnparseablePayloadsFailClosed() {
        LongJobInventory inv = new LongJobInventory();
        inv.setJobData(null);
        assertNull(inv.getJobData());
        inv.setJobData("bad-session-test-secret");
        assertEquals("{}", inv.getJobData());
        inv.setJobData("{}");
        assertEquals("{}", inv.getJobData());
    }
}
