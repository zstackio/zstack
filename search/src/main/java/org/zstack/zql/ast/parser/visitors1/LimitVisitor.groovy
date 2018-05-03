package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class LimitVisitor extends ZQLBaseVisitor<ASTNode.Limit> {
    @Override
    ASTNode.Limit visitLimit(ZQLParser.LimitContext ctx) {
        return new ASTNode.Limit(limit: Long.valueOf(ctx.INT().getText()).intValue())
    }
}
