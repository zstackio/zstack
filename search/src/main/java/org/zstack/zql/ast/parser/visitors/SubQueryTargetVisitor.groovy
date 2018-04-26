package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class SubQueryTargetVisitor extends ZQLBaseVisitor<ASTNode.SubQueryTarget> {
    @Override
    ASTNode.SubQueryTarget visitSubQueryTarget(ZQLParser.SubQueryTargetContext ctx) {
        return new ASTNode.SubQueryTarget(
                entity: ctx.entity().getText(),
                fields: ctx.ID().collect {it.getText()}
        )
    }
}
