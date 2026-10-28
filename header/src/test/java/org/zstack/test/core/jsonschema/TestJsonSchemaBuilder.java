package org.zstack.test.core.jsonschema;

import org.junit.Test;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.JsonSchemaBuilder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestJsonSchemaBuilder {
    public static class DynamicAnswerEvent extends APIEvent {
        private Object answer;

        DynamicAnswerEvent(Object answer) {
            super("api-id");
            this.answer = answer;
        }
    }

    public static class Payload {
        private String value = "value";
    }

    public static class TypedEvent {
        private List<Payload> items = Arrays.asList(new Payload());
        private Map<String, Payload> values = new LinkedHashMap<>();
        private Payload nested = new Payload();

        TypedEvent() {
            values.put("first", new Payload());
        }
    }

    @Test
    public void dynamicImageAnswerTypesDoNotBreakEventSchema() {
        List<String> labels = Arrays.asList("\u53f3\u4fa7", "\u6570\u5b57\u6807\u7b7e-0", "\u542b\u9017\u53f7,\u6807\u7b7e");
        Map<String, Object> customFormat = new LinkedHashMap<>();
        customFormat.put("labels", labels);

        assertFalse(new JsonSchemaBuilder(new DynamicAnswerEvent(labels)).build().containsKey("answer"));
        assertFalse(new JsonSchemaBuilder(new DynamicAnswerEvent(customFormat)).build().containsKey("answer"));
        assertFalse(new JsonSchemaBuilder(new DynamicAnswerEvent("\u53f3\u4fa7")).build().containsKey("answer"));
        assertFalse(new JsonSchemaBuilder(new DynamicAnswerEvent(null)).build().containsKey("answer"));
    }

    @Test
    public void staticallyTypedCollectionsKeepTheirElementSchema() {
        Map<String, String> schema = new JsonSchemaBuilder(new TypedEvent()).build();

        assertEquals(Payload.class.getName(), schema.get("items[0]"));
        assertEquals(Payload.class.getName(), schema.get("values.first"));
        assertEquals(Payload.class.getName(), schema.get("nested"));
    }
}
