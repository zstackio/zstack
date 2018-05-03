package org.zstack.zql1.ast.visitors

import org.zstack.header.zql.ASTVisitor
import org.zstack.zql1.ZQLContext
import org.zstack.header.zql.ASTNode
import org.zstack.zql1.ast.ZQLMetadata

class SubQueryVisitor implements ASTVisitor<String, ASTNode.SubQuery> {
    private String makeConditions(ASTNode.SubQuery node) {
        if (node.conditions?.isEmpty()) {
            return ""
        }

        List<String> conds = node.conditions.collect { (it as ASTNode).accept(new ConditionVisitor()) } as List<String>
        return conds.join(" ")
    }

    @Override
    String visit(ASTNode.SubQuery node) {
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.target.entity)
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
        if (condition != "") {
            clauses.add("WHERE")
            clauses.add(condition)
        }

        ZQLContext.popQueryTargetInventoryName()
        return "(${clauses.join(" ")})"
    }
}
