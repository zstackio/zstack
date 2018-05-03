package org.zstack.zql1.ast.parser.visitors1

import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class OffsetVisitor extends ZQLBaseVisitor<ASTNode.Offset> {
    @Override
    ASTNode.Offset visitOffset(ZQLParser.OffsetContext ctx) {
        return new ASTNode.Offset(offset: Long.valueOf(ctx.INT().getText()).intValue())
    }
}
