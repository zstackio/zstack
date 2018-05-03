package org.zstack.zql1.ast.visitors

import org.zstack.header.zql.ASTVisitor
import org.zstack.zql1.ZQLContext
import org.zstack.header.zql.ASTNode
import org.zstack.zql1.ast.sql.SQLConditionBuilder

class ExprVisitor implements ASTVisitor<String, ASTNode.Expr> {
    @Override
    String visit(ASTNode.Expr node) {
        String inventoryTarget = ZQLContext.peekQueryTargetInventoryName()
        return new SQLConditionBuilder(inventoryTarget, node.left).build(node.operator,
               node.right == null ? "" : (node.right as ASTNode).accept(new ValueVisitor()) as String)
    }
}
