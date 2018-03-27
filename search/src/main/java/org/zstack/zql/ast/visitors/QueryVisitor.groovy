package org.zstack.zql.ast.visitors

import org.zstack.zql.ZQLContext
import org.zstack.zql.ast.ASTNode
import org.zstack.zql.ast.ZQLMetadata
import org.zstack.zql.ast.visitors.result.QueryResult

class QueryVisitor implements ASTVisitor<QueryResult, ASTNode.Query> {
    private String makeConditions(ASTNode.Query node) {
        if (node.conditions?.isEmpty()) {
            return ""
        }

        List<String> conds = node.conditions.collect { (it as ASTNode).accept(new ConditionVisitor()) } as List<String>
        return conds.join(" ")
    }

    QueryResult visit(ASTNode.Query node) {
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.target.entity)
        ZQLContext.pushQueryTargetInventoryName(inventory.fullInventoryName())

        String fieldName = node.target.fields == null || node.target.fields.isEmpty() ? "" : node.target.fields[0]
        if (fieldName != "") {
            inventory.errorIfNoField(fieldName)
        }

        String entityAlias = inventory.simpleInventoryName()
        String queryTarget = fieldName == "" ? entityAlias : "${inventory.simpleInventoryName()}.${fieldName}"
        String entityVOName = inventory.inventoryAnnotation.mappingVOClass().simpleName

        def ret = new QueryResult()
        List<String> clauses = []
        clauses.add("SELECT ${queryTarget} FROM ${entityVOName} ${entityAlias}")
        String condition = makeConditions(node)
        if (condition != "") {
            clauses.add("WHERE")
            clauses.add(condition)
        }

        ret.sql = clauses.join(" ")
        ZQLContext.popQueryTargetInventoryName()
        return ret
    }
}
