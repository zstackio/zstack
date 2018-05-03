package org.zstack.zql1.ast.visitors

import org.zstack.header.zql.ASTNode
import org.zstack.header.zql.ASTVisitor
import org.zstack.zql1.ast.visitors.result.ReturnWithResult

class ReturnWithVisitor implements ASTVisitor<List<ReturnWithResult>, ASTNode.ReturnWith> {
    @Override
    List<ReturnWithResult> visit(ASTNode.ReturnWith node) {
        return node.exprs.collect { (it as ASTNode).accept(new ReturnWithExprVisitor()) }
    }
}
