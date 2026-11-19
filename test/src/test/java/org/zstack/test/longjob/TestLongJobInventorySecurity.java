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
        data.addProperty("message", "{\"message\":\"unterminated");
        data.addProperty("array", "[\"unterminated");
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
    public void bracketPrefixedBusinessTextSurvivesPublicMappingAndProjection() throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("name", "[Ubuntu 22.04] LTS image");
        data.addProperty("description", "[migration] volume ready");
        data.addProperty("message", "[phase 1] {waiting}");
        data.addProperty("details", "[retry-2] connecting");
        data.addProperty("payload", "[phase 1] {waiting}");
        LongJobVO vo = new LongJobVO();
        vo.setJobData(data.toString());
        for (LongJobInventory inv : publicMappings(vo)) {
            assertEquals(data, new JsonParser().parse(inv.getJobData()));
        }
        assertEquals(data.toString(), vo.getJobData());
    }

    @Test
    public void malformedStructuredPayloadsFailClosedInEveryField() throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("payload", "{\"password\":\"fixture-malformed-payload\"");
        data.addProperty("serialized", "[{\"session\":{\"uuid\":\"fixture-malformed-session\"}}");
        data.addProperty("description", "{\"password\":\"fixture-malformed-description\"");
        data.addProperty("message", "[phase 1] {\"apiKey\":\"fixture-malformed-message\"");
        LongJobVO vo = new LongJobVO();
        vo.setJobData(data.toString());
        for (LongJobInventory inv : publicMappings(vo)) {
            JsonObject safe = new JsonParser().parse(inv.getJobData()).getAsJsonObject();
            for (String key : new String[] {"payload", "serialized", "description", "message"}) {
                assertEquals(key, "{}", safe.get(key).getAsString());
            }
        }
        assertEquals(data.toString(), vo.getJobData());
    }

    @Test
    public void imageUrlPasswordsAreNotExposedByPublicMappingOrProjection() throws Exception {
        String[][] urls = {
                {"https://image-user:fixture-url-password@images.example.test:8443/path%20name/image.qcow2?format=qcow2#download",
                        "https://images.example.test:8443/path%20name/image.qcow2?format=qcow2#download"},
                {"https://image%40user:fixture%3Aurl%40password@images.example.test/image.qcow2",
                        "https://images.example.test/image.qcow2"},
                {"http://image-user:fixture-url-password@[2001:db8::1]:8080/image.qcow2",
                        "http://[2001:db8::1]:8080/image.qcow2"},
                {"sftp://image-user:fixture-url-password@images.example.test:2222/image.qcow2",
                        "sftp://images.example.test:2222/image.qcow2"},
                {"https://image-user@images.example.test/image.qcow2",
                        "https://images.example.test/image.qcow2"},
                {"https://image-user:fixture@url@images.example.test/image.qcow2",
                        "https://images.example.test/image.qcow2"}
        };
        for (String[] url : urls) {
            JsonObject payload = new JsonObject();
            payload.addProperty("url", url[0]);
            payload.addProperty("name", "image-with-credentials");
            payload.addProperty("resourceUuid", "fixture-image-uuid");
            JsonObject data = payload.deepCopy();
            data.addProperty("payload", payload.toString());
            LongJobVO vo = new LongJobVO();
            vo.setJobData(data.toString());
            for (LongJobInventory inv : publicMappings(vo)) {
                JsonObject safe = new JsonParser().parse(inv.getJobData()).getAsJsonObject();
                assertEquals(url[1], safe.get("url").getAsString());
                assertEquals(url[1], new JsonParser().parse(safe.get("payload").getAsString())
                        .getAsJsonObject().get("url").getAsString());
                assertEquals("image-with-credentials", safe.get("name").getAsString());
                assertEquals("fixture-image-uuid", safe.get("resourceUuid").getAsString());
            }
            assertEquals(data.toString(), vo.getJobData());
        }
    }

    @Test
    public void ordinaryUrlsAndTextRemainUnchanged() throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("url", "https://images.example.test/path%20name/image.qcow2?format=qcow2#download");
        data.addProperty("uploadUrl", "upload://fixture-resource");
        data.addProperty("description", "contact owner@example.test");
        LongJobVO vo = new LongJobVO();
        vo.setJobData(data.toString());
        for (LongJobInventory inv : publicMappings(vo)) {
            assertEquals(data, new JsonParser().parse(inv.getJobData()));
        }
        assertEquals(data.toString(), vo.getJobData());
    }

    @Test
    public void unstructuredContainerLikeTextIsPreservedExactly() throws Exception {
        for (String text : new String[] {"[waiting]", "{waiting}", "{not-json", "[unterminated",
                "  [phase 1] {waiting}  ", "[training] waiting"}) {
            JsonObject data = new JsonObject();
            data.addProperty("value", text);
            LongJobVO vo = new LongJobVO();
            vo.setJobData(data.toString());
            for (LongJobInventory inv : publicMappings(vo)) {
                assertEquals(text, new JsonParser().parse(inv.getJobData()).getAsJsonObject()
                        .get("value").getAsString());
            }
            assertEquals(data.toString(), vo.getJobData());
        }
    }

    @Test
    public void malformedUrlUserInfoFailsClosedWhileNonCredentialTextIsPreserved() throws Exception {
        for (String url : new String[] {"https://image-user:fixture url password@images.example.test/image.qcow2",
                "https://image-user:fixture%zz@images.example.test/image.qcow2",
                "https://image-user:fixture-url-password@[invalid/image.qcow2"}) {
            JsonObject data = new JsonObject();
            data.addProperty("url", url);
            LongJobVO vo = new LongJobVO();
            vo.setJobData(data.toString());
            for (LongJobInventory inv : publicMappings(vo)) {
                assertEquals("{}", new JsonParser().parse(inv.getJobData()).getAsJsonObject()
                        .get("url").getAsString());
            }
            assertEquals(data.toString(), vo.getJobData());
        }
    }

    @Test
    public void nestedJsonStringEncodingCannotBypassCredentialFiltering() throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("password", "fixture-layered-password");
        payload.addProperty("resourceUuid", "fixture-layered-resource");
        String serialized = payload.toString();
        for (int layers = 1; layers <= 3; layers++) {
            serialized = new com.google.gson.JsonPrimitive(serialized).toString();
            JsonObject data = new JsonObject();
            data.addProperty("payload", serialized);
            LongJobVO vo = new LongJobVO();
            vo.setJobData(data.toString());
            for (LongJobInventory inv : publicMappings(vo)) {
                assertFalse(inv.getJobData().contains("fixture-layered-password"));
                assertTrue(inv.getJobData().contains("fixture-layered-resource"));
            }
            assertEquals(data.toString(), vo.getJobData());
        }
        for (String text : new String[] {"{\\\"password\\\":\\\"fixture-broken-encoding", "[1] ready",
                "[\"fixture-broken-array", "[true", "[nul", "{", "["}) {
            JsonObject data = new JsonObject();
            data.addProperty("payload", text);
            LongJobVO vo = new LongJobVO();
            vo.setJobData(data.toString());
            for (LongJobInventory inv : publicMappings(vo)) {
                assertEquals("{}", new JsonParser().parse(inv.getJobData()).getAsJsonObject()
                        .get("payload").getAsString());
            }
            assertEquals(data.toString(), vo.getJobData());
        }
    }

    @Test
    public void authorityWithoutUserInfoKeepsAtSignsInThePathOrQuery() throws Exception {
        for (String url : new String[] {"https://images_internal.example.test/path?owner=a@b",
                "https://images_internal.example.test/owner@site/path%20name#part",
                "  HtTpS://[2001:db8::1]:8443/path%20name?owner=a@b#download  "}) {
            JsonObject data = new JsonObject();
            data.addProperty("url", url);
            LongJobVO vo = new LongJobVO();
            vo.setJobData(data.toString());
            for (LongJobInventory inv : publicMappings(vo)) {
                assertEquals(url, new JsonParser().parse(inv.getJobData()).getAsJsonObject()
                        .get("url").getAsString());
            }
            assertEquals(data.toString(), vo.getJobData());
        }
    }

    @Test
    public void leadingEscapedJsonContainersFailClosedWithoutChangingOrdinaryBackslashes() throws Exception {
        for (String text : new String[] {"\\\"{\\\"password\\\":\\\"fixture-broken-password",
                "\\\\\\\"[\\\\\\\"fixture-broken-array"}) {
            JsonObject data = new JsonObject();
            data.addProperty("payload", text);
            LongJobVO vo = new LongJobVO();
            vo.setJobData(data.toString());
            for (LongJobInventory inv : publicMappings(vo)) {
                assertEquals("{}", new JsonParser().parse(inv.getJobData()).getAsJsonObject()
                        .get("payload").getAsString());
            }
            assertEquals(data.toString(), vo.getJobData());
        }
        for (String text : new String[] {"\\waiting", "\\\\server\\share", "\\\"quoted text"}) {
            JsonObject data = new JsonObject();
            data.addProperty("payload", text);
            LongJobVO vo = new LongJobVO();
            vo.setJobData(data.toString());
            for (LongJobInventory inv : publicMappings(vo)) {
                assertEquals(text, new JsonParser().parse(inv.getJobData()).getAsJsonObject()
                        .get("payload").getAsString());
            }
            assertEquals(data.toString(), vo.getJobData());
        }
    }

    private LongJobInventory[] publicMappings(LongJobVO vo) throws Exception {
        LongJobInventory projected = new LongJobInventory();
        PropertyUtils.setProperty(projected, "jobData", vo.getJobData());
        return new LongJobInventory[] {LongJobInventory.valueOf(vo), projected};
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
