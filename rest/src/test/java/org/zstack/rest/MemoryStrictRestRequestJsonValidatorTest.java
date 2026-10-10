package org.zstack.rest;

import com.google.gson.JsonParseException;
import org.junit.Test;
import org.zstack.header.rest.StrictRestRequestJson;
import static org.junit.Assert.assertThrows;

public class MemoryStrictRestRequestJsonValidatorTest {
    @StrictRestRequestJson
    static class StrictFixture {
        public Object action;
        public Object details;
    }

    static class UnmarkedFixture {
        public Object action;
        public Object details;
    }

    @Test public void rejectsNestedDuplicateKeysFromRawActionTokens() {
        assertThrows(JsonParseException.class, () -> StrictRestRequestJsonValidator.validateIfOptedIn(
                "{\"updateMemoryPolicy\":{\"details\":{\"pool\":1,\"pool\":2}}}",
                "updateMemoryPolicy", StrictFixture.class));
    }

    @Test public void rejectsDuplicateSelectedWrapper() {
        assertThrows(JsonParseException.class, () -> StrictRestRequestJsonValidator.validateIfOptedIn(
                "{\"updateMemoryPolicy\":{},\"updateMemoryPolicy\":{}}",
                "updateMemoryPolicy", StrictFixture.class));
    }

    @Test public void rejectsRepeatedApiFieldsBeforeReflectiveGsonCanCollapseThem() {
        assertThrows(JsonParseException.class, () -> StrictRestRequestJsonValidator.validateIfOptedIn(
                "{\"updateMemoryPolicy\":{\"action\":\"apply\",\"action\":\"pause\"}}",
                "updateMemoryPolicy", StrictFixture.class));
    }

    @Test public void doesNotParseOrChangeUnmarkedApis() {
        StrictRestRequestJsonValidator.validateIfOptedIn(
                "{\"updateMemoryPolicy\":{\"details\":{\"a\":1,\"a\":2}}}",
                "updateMemoryPolicy", UnmarkedFixture.class);
    }
}
