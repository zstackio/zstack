package org.zstack.zql.ast.visitors

import org.zstack.zql.ZQLContext
import org.zstack.zql.ast.ASTNode
import org.zstack.zql.ast.ZQLMetadata
import org.zstack.zql.ast.visitors.result.QueryResult

class QueryVisitor implements ASTVisitor<QueryResult, ASTNode.Query> {
    def ret = new QueryResult()

    private String makeConditions(ASTNode.Query node) {
        if (node.conditions?.isEmpty()) {
            return ""
        }

        List<String> conds = node.conditions.collect { (it as ASTNode).accept(new ConditionVisitor()) } as List<String>
        return conds.join(" ")
    }

    private String makeSQL(ASTNode.Query node) {
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.target.entity)
        ret.inventoryMetadata = inventory
        ZQLContext.pushQueryTargetInventoryName(inventory.fullInventoryName())

        String fieldName = node.target.fields == null || node.target.fields.isEmpty() ? "" : node.target.fields[0]
        if (fieldName != "") {
            inventory.errorIfNoField(fieldName)
        }

        String entityAlias = inventory.simpleInventoryName()
        String queryTarget = fieldName == "" ? entityAlias : "${inventory.simpleInventoryName()}.${fieldName}"
        String entityVOName = inventory.inventoryAnnotation.mappingVOClass().simpleName

        List<String> clauses = []
        clauses.add("SELECT ${queryTarget} FROM ${entityVOName} ${entityAlias}")
        String condition = makeConditions(node)
        String restrictBy = node.restrictBy?.accept(new RestrictByVisitor())
        if (condition != "" || restrictBy != null) {
            clauses.add("WHERE")
        }

        List<String> conditionClauses = []
        if (condition != "") {
            conditionClauses.add(condition)
        }
        if (restrictBy != null) {
            conditionClauses.add(restrictBy)
        }

        if (!conditionClauses.isEmpty()) {
            clauses.add(conditionClauses.join(" AND "))
        }

        if (node.orderBy != null) {
            clauses.add(node.orderBy.accept(new OrderByVisitor()) as String)
        }

        if (node.limit != null) {
            clauses.add(node.limit.accept(new LimitVisitor()) as String)
        }

        if (node.offset != null) {
            clauses.add(node.offset.accept(new OffsetVisitor()) as String)
        }

        ZQLContext.popQueryTargetInventoryName()
        return clauses.join(" ")
    }

    QueryResult visit(ASTNode.Query node) {
        ret.sql = makeSQL(node)
        return ret
    }
}
