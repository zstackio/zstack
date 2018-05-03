package org.zstack.zql1.ast.parser.visitors1

import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class SubQueryVisitor extends ZQLBaseVisitor<ASTNode.SubQuery> {
    ASTNode.SubQuery visitSubQuery(ZQLParser.SubQueryContext ctx) {
        return new ASTNode.SubQuery(
                target: ctx.subQueryTarget().accept(new SubQueryTargetVisitor()),
                conditions: ctx.condition().collect {it.accept(new ConditionVisitor())}
        )
    }
}
