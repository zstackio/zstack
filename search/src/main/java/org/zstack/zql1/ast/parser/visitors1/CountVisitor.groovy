package org.zstack.zql1.ast.parser.visitors1

import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class CountVisitor extends ZQLBaseVisitor<ASTNode.Query> {
    @Override
    ASTNode.Query visitCount(ZQLParser.CountContext ctx) {
        return new ASTNode.Query(
                target: ctx.queryTarget().accept(new QueryTargetVisitor()),
                conditions: ctx.condition()?.collect {it.accept(new ConditionVisitor())},
                restrictBy: ctx.restrictBy()?.accept(new RestrictByVisitor()),
                orderBy: ctx.orderBy()?.accept(new OrderByVisitor()),
                limit: ctx.limit()?.accept(new LimitVisitor()),
                offset: ctx.offset()?.accept(new OffsetVisitor())
        )
    }
}
