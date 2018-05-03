package org.zstack.zql.sql;

import groovy.text.SimpleTemplateEngine;
import org.zstack.zql1.ast.ZQLMetadata;

import java.util.Iterator;
import java.util.List;

public class SQLConditionBuilder {
    private List<String> conditionNames
    private String template

    private static final OPERATOR_NAME = "__operatorName__"
    private static final VALUE_NAME = "__valueName__"

    public SQLConditionBuilder(String queryTargetInventoryName, List<String> conditionNames) {
        this.conditionNames = conditionNames

        def chainQueries = ZQLMetadata.createChainQuery(queryTargetInventoryName, conditionNames)
        if (chainQueries.size() == 1) {
            ZQLMetadata.FieldChainQuery fc = chainQueries[0] as ZQLMetadata.FieldChainQuery
                    template = "${fc.self.selfInventoryClass.simpleName}.${fc.fieldName} \${${OPERATOR_NAME}} \${${VALUE_NAME}}"
        } else {
            ZQLMetadata.ExpandChainQuery first = chainQueries[0] as ZQLMetadata.ExpandChainQuery
                    template = "${first.self.simpleInventoryName()}.${first.right.selfKeyName} IN ${makeTemplate(chainQueries[1..chainQueries.size()-1].iterator())}"
        }
    }

    private String makeTemplate(Iterator<ZQLMetadata.ChainQueryStruct> iterator) {
        ZQLMetadata.ChainQueryStruct current = iterator.next()

        String value = iterator.hasNext() ? makeTemplate(iterator) : null

        if (value == null) {
            assert current instanceof ZQLMetadata.FieldChainQuery : "the last pair is not a FieldChainQuery"

            ZQLMetadata.ExpandQueryMetadata right = current.right
            String entityName = right.targetInventoryClass.simpleName
            return "(SELECT ${entityName}.${right.targetKeyName} FROM ${right.targetVOClass.simpleName}" +
                    " ${entityName} WHERE ${entityName}.${current.fieldName} \${${OPERATOR_NAME}} \${${VALUE_NAME}})"
        }

        current = current as ZQLMetadata.ExpandChainQuery

        ZQLMetadata.ExpandQueryMetadata right = current.right
        String entityName = current.self.selfInventoryClass.simpleName
        return "(SELECT ${entityName}.${current.selfKey} FROM ${right.selfVOClass.simpleName} ${entityName}" +
                " WHERE ${entityName}.${right.selfKeyName} IN ${value})"
    }

    public String build(String operator, String value) {
        SimpleTemplateEngine engine = new SimpleTemplateEngine()
        return engine.createTemplate(template).make([(OPERATOR_NAME): operator, (VALUE_NAME): value]).toString()
    }
}
