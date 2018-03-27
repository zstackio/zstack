package org.zstack.zql.ast.visitors

import org.zstack.zql.ZQLContext
import org.zstack.zql.ast.ASTNode
import org.zstack.zql.ast.sql.SQLConditionBuilder

class ExprVisitor implements ASTVisitor<String, ASTNode.Expr> {
    @Override
    String visit(ASTNode.Expr node) {
        String inventoryTarget = ZQLContext.peekQueryTargetInventoryName()
        return new SQLConditionBuilder(inventoryTarget, node.left).build(node.operator,
                (node.right as ASTNode).accept(new ValueVisitor()) as String)
    }
}
