package org.zstack.zql.ast.parser.visitors

import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode

class OffsetVisitor extends ZQLBaseVisitor<ASTNode.Offset> {
    @Override
    ASTNode.Offset visitOffset(ZQLParser.OffsetContext ctx) {
        return new ASTNode.Offset(offset: Long.valueOf(ctx.INT().getText()).intValue())
    }
}
