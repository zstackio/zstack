package org.zstack.test.longjob;

import org.junit.Test;
import org.zstack.header.longjob.LongJobInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.zql.ZQL;
import org.zstack.zql.ZQLQueryReturn;
import org.zstack.zql.ast.ZQLMetadata;
import org.zstack.zql.ast.visitors.result.QueryResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class TestLongJobAggregateSecurity {
    private static final String RAW = "{\"session\":{\"uuid\":\"fixture-aggregate-session\"},\"name\":\"download\"}";

    private ZQL query(Class<?> inventoryClass, String... fields) throws Exception {
        QueryResult result = new QueryResult();
        result.targetFieldNames = Arrays.asList(fields);
        result.inventoryMetadata = new ZQLMetadata.InventoryMetadata();
        result.inventoryMetadata.selfInventoryClass = inventoryClass;
        ZQL zql = new ZQL();
        Field astResult = ZQL.class.getDeclaredField("astResult");
        astResult.setAccessible(true);
        astResult.set(zql, result);
        return zql;
    }

    private Map<?, ?> project(Class<?> inventoryClass, Object values, String... fields) throws Exception {
        ZQL zql = query(inventoryClass, fields);
        Method mapping = ZQL.class.getDeclaredMethod("entityVOtoMap", Object.class);
        mapping.setAccessible(true);
        return (Map<?, ?>) mapping.invoke(zql, values);
    }

    private Object aggregate(Object row, String... fields) throws Exception {
        ZQL zql = query(LongJobInventory.class, fields);
        ZQLQueryReturn result = new ZQLQueryReturn();
        result.inventoryAggregateFunctions = new LinkedHashMap<>();
        Method mapping = ZQL.class.getDeclaredMethod("addAggregateResult", ZQLQueryReturn.class, Object.class);
        mapping.setAccessible(true);
        mapping.invoke(zql, result, row);
        return result.inventoryAggregateFunctions.values().iterator().next();
    }

    @Test
    public void aggregateValuesCannotBypassPublicJobDataProtection() throws Exception {
        assertFalse(aggregate(RAW, "jobData").toString().contains("fixture-aggregate-session"));
        assertFalse(aggregate(new Object[] {"owner", RAW}, "accountUuid", "jobData").toString()
                .contains("fixture-aggregate-session"));
        assertEquals(4L, aggregate(new Object[] {RAW, 4L}, "jobData", "size"));
    }

    @Test
    public void groupedLongJobFieldsDoNotExposeCredentials() throws Exception {
        Object[] row = {RAW, 4L};
        Map<?, ?> mapped = project(LongJobInventory.class, row, "jobData", "size");
        assertFalse(mapped.get("jobData").toString().contains("fixture-aggregate-session"));
        assertTrue(mapped.get("jobData").toString().contains("download"));
        assertEquals(4L, mapped.get("size"));
        assertEquals("query projection must not mutate internal job data", RAW, row[0]);
    }

    @Test
    public void scalarLongJobProjectionUsesTheSameSanitizer() throws Exception {
        assertFalse(project(LongJobInventory.class, RAW, "jobData").get("jobData").toString()
                .contains("fixture-aggregate-session"));
    }

    @Test
    public void unrelatedInventoryFieldsRemainUnchanged() throws Exception {
        assertEquals(RAW, project(VmInstanceInventory.class, RAW, "name").get("name"));
    }
}
