package org.zstack.zql.ast.sql

class SQLConditionBuilder {
    private List<String> conditionNames
    private String template

    SQLConditionBuilder(List<String> conditionNames) {
        this.conditionNames = conditionNames
        template = makeTemplate(conditionNames.iterator())
    }

    private String makeTemplate(Iterator<String> it) {
    }

    String build(String operator, String value) {
    }
}
