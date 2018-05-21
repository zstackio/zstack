package org.zstack.zql.sql;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.query.Queryable;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.zql.ast.ZQLMetadata;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SQLConditionBuilder {
    private String template;
    private Field conditionField;

    private static class QueryableField {
        Queryable annotation;
        Class inventoryClass;
        Field field;

        String toSQL() {
            ZQLMetadata.InventoryMetadata metadata = ZQLMetadata.getInventoryMetadataByName(inventoryClass.getName());
            if (metadata == null) {
                throw new CloudRuntimeException(String.format("cannot find InventoryMetadata for class[%s]", inventoryClass));
            }

            Field primaryKey = EntityMetadata.getPrimaryKeyField(metadata.inventoryAnnotation.mappingVOClass());
            Class mappingInventoryClass = annotation.mappingClass();
            ZQLMetadata.InventoryMetadata mappingInventoryMetadata = ZQLMetadata.getInventoryMetadataByName(mappingInventoryClass.getName());
            String mappingEntityName = mappingInventoryClass.getSimpleName();

            return String.format("%s.%s IN (SELECT %s.%s FROM %s %s WHERE %s.%s %%s %%s)",
                    inventoryClass.getSimpleName(), primaryKey.getName(), mappingEntityName,
                    annotation.joinColumn().name(), mappingInventoryMetadata.inventoryAnnotation.mappingVOClass().getSimpleName(),
                    mappingEntityName, mappingEntityName, annotation.joinColumn().referencedColumnName());
        }
    }

    private static Map<String, QueryableField> queryableFields = new HashMap<>();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getFieldsAnnotatedWith(Queryable.class).forEach(f->{
            QueryableField qf = new QueryableField();
            qf.annotation = f.getAnnotation(Queryable.class);
            qf.field = f;
            qf.inventoryClass = f.getDeclaringClass();
            queryableFields.put(String.format("%s.%s", qf.inventoryClass.getName(), f.getName()), qf);
        });
    }

    private void setConditionField(Class clz, String fname) {
        conditionField = FieldUtils.getField(fname, clz);
        if (conditionField == null) {
            throw new CloudRuntimeException(String.format("inventory class[%s] has no field[%s]", clz, fname));
        }
    }

    private QueryableField getIfConditionFieldQueryableField() {
        return queryableFields.get(String.format("%s.%s", conditionField.getDeclaringClass().getName(), conditionField.getName()));
    }

    public SQLConditionBuilder(String queryTargetInventoryName, List<String> conditionNames) {
        List<ZQLMetadata.ChainQueryStruct> chainQueries = ZQLMetadata.createChainQuery(queryTargetInventoryName, conditionNames);
        if (chainQueries.size() == 1) {
            ZQLMetadata.FieldChainQuery fc = (ZQLMetadata.FieldChainQuery) chainQueries.get(0);
            setConditionField(fc.self.selfInventoryClass, fc.fieldName);
            QueryableField qf = getIfConditionFieldQueryableField();

            if (qf == null) {
                template = String.format("%s.%s %%s %%s", fc.self.selfInventoryClass.getSimpleName(), fc.fieldName);
            } else {
                template = qf.toSQL();
            }
        } else {
            ZQLMetadata.ExpandChainQuery first = (ZQLMetadata.ExpandChainQuery) chainQueries.get(0);
            template = String.format("%s.%s IN %s",
                    first.self.simpleInventoryName(), first.right.selfKeyName, makeTemplate(chainQueries.subList(1, chainQueries.size()).iterator()));
        }
    }

    private String makeTemplate(Iterator<ZQLMetadata.ChainQueryStruct> iterator) {
        ZQLMetadata.ChainQueryStruct current = iterator.next();

        String value = iterator.hasNext() ? makeTemplate(iterator) : null;

        if (value == null) {
            if (!(current instanceof ZQLMetadata.FieldChainQuery)) {
                throw new CloudRuntimeException("the last pair is not a FieldChainQuery");
            }

            ZQLMetadata.FieldChainQuery fc = (ZQLMetadata.FieldChainQuery) current;
            ZQLMetadata.ExpandQueryMetadata right = fc.right;
            String entityName = right.targetInventoryClass.getSimpleName();

            setConditionField(right.targetInventoryClass, fc.fieldName);

            QueryableField qf = getIfConditionFieldQueryableField();

            if (qf == null) {
                return String.format("(SELECT %s.%s FROM %s" +
                                " %s WHERE %s.%s %%s %%s)",
                        entityName, right.targetKeyName, right.targetVOClass.getSimpleName(), entityName, entityName,
                        fc.fieldName);
            } else {
                return String.format("(SELECT %s.%s FROM %s" +
                                " %s WHERE %s)",
                        entityName, right.targetKeyName, right.targetVOClass.getSimpleName(), entityName, qf.toSQL());
            }
        }

        ZQLMetadata.ExpandChainQuery ec = (ZQLMetadata.ExpandChainQuery) current;

        ZQLMetadata.ExpandQueryMetadata right = ec.right;
        String entityName = ec.self.selfInventoryClass.getSimpleName();
        return String.format("(SELECT %s.%s FROM %s %s" +
                " WHERE %s.%s IN %s)",
                entityName, ec.selfKey, right.selfVOClass.getSimpleName(), entityName,
                entityName, right.selfKeyName, value);
    }

    private String normalizeValue(String value) {
        if (Boolean.class.isAssignableFrom(conditionField.getType()) || boolean.class.isAssignableFrom(conditionField.getType())) {
            return StringUtils.strip(value, "'");
        } else {
            return value;
        }
    }

    public String build(String operator, String value) {
        return String.format(template, operator, normalizeValue(value));
    }
}
