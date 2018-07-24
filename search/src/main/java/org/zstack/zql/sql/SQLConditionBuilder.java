package org.zstack.zql.sql;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.EntityMetadata;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.query.Queryable;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.UserTagVO;
import org.zstack.header.zql.ASTNode;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.zql.ZQL;
import org.zstack.zql.ast.ZQLMetadata;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SQLConditionBuilder {
    private String template;
    private Field conditionField;
    private String operator;
    private String value;

    private enum ConditionType {
        QueryableField,
        Tag,
        Normal
    }

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
    private static Map<String, String> TAG_FALSE_OP = new HashMap();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getFieldsAnnotatedWith(Queryable.class).forEach(f->{
            QueryableField qf = new QueryableField();
            qf.annotation = f.getAnnotation(Queryable.class);
            qf.field = f;
            qf.inventoryClass = f.getDeclaringClass();
            queryableFields.put(String.format("%s.%s", qf.inventoryClass.getName(), f.getName()), qf);
        });

        TAG_FALSE_OP.put("!=", "=");
        TAG_FALSE_OP.put("not in", "in");
        TAG_FALSE_OP.put("not like", "like");
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

    private ConditionType getConditionType(Class invClass, String fname) {
        if (fname.equals(ZQLMetadata.SYS_TAG_NAME) || fname.equals(ZQLMetadata.USER_TAG_NAME)) {
            return ConditionType.Tag;
        } else if (queryableFields.containsKey(String.format("%s.%s", invClass.getName(), fname))) {
            return ConditionType.QueryableField;
        } else {
            return ConditionType.Normal;
        }
    }

    public SQLConditionBuilder(String queryTargetInventoryName, List<String> conditionNames, String operator, String value) {
        this.operator = operator;
        this.value = value;

        List<ZQLMetadata.ChainQueryStruct> chainQueries = ZQLMetadata.createChainQuery(queryTargetInventoryName, conditionNames);
        if (chainQueries.size() == 1) {
            ZQLMetadata.FieldChainQuery fc = (ZQLMetadata.FieldChainQuery) chainQueries.get(0);

            ConditionType ctype = getConditionType(fc.self.selfInventoryClass, fc.fieldName);
            if (ctype == ConditionType.Tag) {
                template = createTagSQL(fc.self.selfInventoryClass, fc.fieldName, false);
            } else {
                setConditionField(fc.self.selfInventoryClass, fc.fieldName);
                QueryableField qf = getIfConditionFieldQueryableField();

                if (qf == null) {
                    template = String.format("%s.%s %%s %%s", fc.self.selfInventoryClass.getSimpleName(), fc.fieldName);
                } else {
                    // self may be children class
                    qf.inventoryClass = fc.self.selfInventoryClass;
                    template = qf.toSQL();
                }
            }
        } else {
            ZQLMetadata.ExpandChainQuery first = (ZQLMetadata.ExpandChainQuery) chainQueries.get(0);
            template = String.format("%s.%s IN %s",
                    first.self.simpleInventoryName(), first.right.selfKeyName, makeTemplate(chainQueries.subList(1, chainQueries.size()).iterator()));
        }

        if (template.contains("%s")) {
            // unresolved SQL, resolve it
            template = String.format(template, operator, normalizeValue(value));
        }
    }

    private String createTagSQL(Class invClz, String fieldName, boolean nestedQuery) {
        class TagSQLMaker {
            String make() {
                ZQLMetadata.InventoryMetadata src = ZQLMetadata.getInventoryMetadataByName(invClz.getName());

                String primaryKey = EntityMetadata.getPrimaryKeyField(src.inventoryAnnotation.mappingVOClass()).getName();
                String tableName = fieldName.equals(ZQLMetadata.USER_TAG_NAME) ? UserTagVO.class.getSimpleName() : SystemTagVO.class.getSimpleName();

                String subCondition;
                if (TAG_FALSE_OP.containsKey(operator)) {
                    String reserveOp = TAG_FALSE_OP.get(operator);
                    subCondition = String.format("tagvo.uuid IN (SELECT tagvo_.uuid FROM %s tagvo_ WHERE tagvo_.tag %s %s)",
                            tableName, reserveOp, value);

                    if (nestedQuery) {
                        return String.format("(SELECT %s.%s FROM %s %s WHERE %s.%s NOT IN (SELECT tagvo.resourceUuid FROM %s tagvo WHERE %s))",
                                src.simpleInventoryName(), primaryKey, src.inventoryAnnotation.mappingVOClass().getSimpleName(),
                                src.simpleInventoryName() , src.simpleInventoryName(), primaryKey, tableName, subCondition);
                    } else {
                        return String.format("(%s.%s NOT IN (SELECT tagvo.resourceUuid FROM %s tagvo WHERE %s))",
                                src.simpleInventoryName(), primaryKey, tableName, subCondition);
                    }
                } else {
                    if (value == null) {
                        subCondition = String.format("tagvo.tag %s", operator);
                    } else {
                        subCondition = String.format("tagvo.tag %s %s", operator, value);
                    }

                    if (nestedQuery) {
                        return String.format("(SELECT %s.%s FROM %s %s WHERE %s.%s IN (SELECT tagvo.resourceUuid FROM %s tagvo WHERE %s))",
                                src.simpleInventoryName(), primaryKey, src.inventoryAnnotation.mappingVOClass().getSimpleName(), src.simpleInventoryName(),
                                src.simpleInventoryName(), primaryKey, tableName, subCondition);
                    } else {
                        return String.format("(%s.%s IN (SELECT tagvo.resourceUuid FROM %s tagvo WHERE %s))",
                                src.simpleInventoryName(), primaryKey, tableName, subCondition);
                    }
                }
            }
        }

        return new TagSQLMaker().make();
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

            ConditionType ctype = getConditionType(right.targetInventoryClass, fc.fieldName);
            if (ctype == ConditionType.Tag) {
                return createTagSQL(right.targetInventoryClass, fc.fieldName, true);
            } else {
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

    public String build() {
        return template;
    }
}
