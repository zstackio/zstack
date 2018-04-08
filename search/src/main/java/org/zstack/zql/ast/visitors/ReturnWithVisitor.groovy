package org.zstack.zql.ast.visitors

import org.zstack.header.zql.ASTNode
import org.zstack.header.zql.ASTVisitor
import org.zstack.zql.ast.visitors.result.ReturnWithResult

class ReturnWithVisitor implements ASTVisitor<List<ReturnWithResult>, ASTNode.ReturnWith> {
    @Override
    List<ReturnWithResult> visit(ASTNode.ReturnWith node) {
        return node.exprs.collect { (it as ASTNode).accept(new ReturnWithExprVisitor()) }
    }
}
