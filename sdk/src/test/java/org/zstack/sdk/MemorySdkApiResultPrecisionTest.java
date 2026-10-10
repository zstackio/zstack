package org.zstack.sdk;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/** Covers the shared schema-remapping path used by generated memory DTOs. */
public class MemorySdkApiResultPrecisionTest {
    private static final String SOURCE = "test.memory.sdk.PrecisionInventory";
    private String previous;

    public static class Item {
        public long value;
        public BigDecimal decimal;
        public Boolean flag;
        public String text;
        public String absent;
        public List children;
        public List getChildren() { return children; }
        public void setChildren(List value) { children = value; }
    }

    public static class Result {
        public List items;
        public Map named;
        public Item typed;
        public List getItems() { return items; }
        public void setItems(List value) { items = value; }
        public Map getNamed() { return named; }
        public void setNamed(Map value) { named = value; }
        public Item getTyped() { return typed; }
        public void setTyped(Item value) { typed = value; }
    }

    @Before public void registerTestMapping() {
        previous = SourceClassMap.srcToDstMapping.put(SOURCE, Item.class.getName());
    }

    @After public void restoreMapping() {
        if (previous == null) SourceClassMap.srcToDstMapping.remove(SOURCE);
        else SourceClassMap.srcToDstMapping.put(SOURCE, previous);
    }

    private Result parse(String json) {
        ApiResult result = new ApiResult();
        result.setResultString(json);
        return result.getResult(Result.class);
    }

    @Test public void listRemappingKeepsLongDecimalBooleanStringAndNull() {
        Result result = parse("{\"items\":[{\"value\":9007199254740993,"
                + "\"decimal\":0.12345678901234567890123456789,\"flag\":true,"
                + "\"text\":\"9007199254740993\",\"absent\":null}],"
                + "\"schema\":{\"items[0]\":\"" + SOURCE + "\"}}");
        Item item = (Item) result.items.get(0);
        assertEquals(9007199254740993L, item.value);
        assertEquals(new BigDecimal("0.12345678901234567890123456789"), item.decimal);
        assertEquals(Boolean.TRUE, item.flag);
        assertEquals("9007199254740993", item.text);
        assertNull(item.absent);
    }

    @Test public void namedMapRemappingKeepsSignedLongExtremes() {
        Result result = parse("{\"named\":{\"minimum\":{\"value\":-9223372036854775808},"
                + "\"maximum\":{\"value\":9223372036854775807}},\"schema\":{"
                + "\"named.minimum\":\"" + SOURCE + "\",\"named.maximum\":\"" + SOURCE + "\"}}");
        assertEquals(Long.MIN_VALUE, ((Item) result.named.get("minimum")).value);
        assertEquals(Long.MAX_VALUE, ((Item) result.named.get("maximum")).value);
    }

    @Test public void remappedParentAndChildUseOriginalNumberTokens() {
        Result result = parse("{\"items\":[{\"value\":9007199254740993,\"children\":[{"
                + "\"value\":-9007199254740993}]}],\"schema\":{\"items[0]\":\"" + SOURCE
                + "\",\"items[0].children[0]\":\"" + SOURCE + "\"}}");
        Item parent = (Item) result.items.get(0);
        assertEquals(9007199254740993L, parent.value);
        assertEquals(-9007199254740993L, ((Item) parent.children.get(0)).value);
    }

    @Test public void alreadyTypedAndNoSchemaResultsRemainUnchanged() {
        String data = "\"typed\":{\"value\":9007199254740993,\"flag\":false}";
        assertEquals(9007199254740993L, parse("{" + data + "}").typed.value);
        Result result = parse("{" + data + ",\"schema\":{\"typed\":\"" + SOURCE + "\"}}");
        assertEquals(9007199254740993L, result.typed.value);
        assertEquals(Boolean.FALSE, result.typed.flag);
    }

    @Test public void unknownSchemaDoesNotChangeRawMapContract() {
        Result result = parse("{\"items\":[{\"value\":5.5}],\"schema\":{"
                + "\"items[0]\":\"test.unregistered.Inventory\"}}");
        assertTrue(result.items.get(0) instanceof Map);
        assertEquals(Double.valueOf(5.5), ((Map) result.items.get(0)).get("value"));
    }

    @Test public void absentPropertyInSchemaRemainsIgnored() {
        Result result = parse("{\"typed\":{\"value\":7},\"schema\":{\"missing\":\"" + SOURCE + "\"}}");
        assertEquals(7L, result.typed.value);
    }

    @Test public void emptyResultRemainsNull() {
        assertNull(new ApiResult().getResult(Result.class));
        assertNull(parse(""));
    }

    @Test public void exponentAndNullSchemaTargetKeepTheirMeaning() {
        Result result = parse("{\"items\":[null,{\"value\":9007199254740993,"
                + "\"decimal\":1.234567890123456789e-30}],\"schema\":{\"items[0]\":\"" + SOURCE
                + "\",\"items[1]\":\"" + SOURCE + "\"}}");
        assertNull(result.items.get(0));
        Item item = (Item) result.items.get(1);
        assertEquals(9007199254740993L, item.value);
        assertEquals(new BigDecimal("1.234567890123456789e-30"), item.decimal);
    }
}
