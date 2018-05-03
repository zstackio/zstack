package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class QueryTargetVisitor extends ZQLBaseVisitor<ASTNode.QueryTarget> {
    ASTNode.QueryTarget visitQueryTarget(ZQLParser.QueryTargetContext ctx) {
        return new ASTNode.QueryTarget(
                entity: ctx.entity().ID().getText(),
                fields: ctx.field() == null ? null : ctx.field().ID().collect { it.getText() }
        )
    }
}
