package org.zstack.zql.sql;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.zql.ast.ZQLMetadata;

import java.util.Iterator;
import java.util.List;

public class SQLConditionBuilder {
    private List<String> conditionNames;
    private String template;


    public SQLConditionBuilder(String queryTargetInventoryName, List<String> conditionNames) {
        this.conditionNames = conditionNames;

        List<ZQLMetadata.ChainQueryStruct> chainQueries = ZQLMetadata.createChainQuery(queryTargetInventoryName, conditionNames);
        if (chainQueries.size() == 1) {
            ZQLMetadata.FieldChainQuery fc = (ZQLMetadata.FieldChainQuery) chainQueries.get(0);
            template = String.format("%s.%s %%s %%s",
                    fc.self.selfInventoryClass.getSimpleName(), fc.fieldName);
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
            return String.format("(SELECT %s.%s FROM %s" +
                    " %s WHERE %s.%s %%s %%s)",
                    entityName, right.targetKeyName, right.targetVOClass.getSimpleName(), entityName, entityName,
                    ((ZQLMetadata.FieldChainQuery) current).fieldName);
        }

        ZQLMetadata.ExpandChainQuery ec = (ZQLMetadata.ExpandChainQuery) current;

        ZQLMetadata.ExpandQueryMetadata right = ec.right;
        String entityName = ec.self.selfInventoryClass.getSimpleName();
        return String.format("(SELECT %s.%s FROM %s %s" +
                " WHERE %s.%s IN %s)",
                entityName, ec.selfKey, right.selfVOClass.getSimpleName(), entityName,
                entityName, right.selfKeyName, value);
    }

    public String build(String operator, String value) {
        return String.format(template, operator, value);
    }
}
