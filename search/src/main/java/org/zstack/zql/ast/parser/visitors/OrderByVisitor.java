package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

public class OrderByVisitor extends ZQLBaseVisitor<ASTNode.OrderBy> {
    @Override
    public ASTNode.OrderBy visitOrderBy(ZQLParser.OrderByContext ctx) {
        ASTNode.OrderBy o = new ASTNode.OrderBy();
        o.setDirection(ctx.ORDER_BY_VALUE().getText());
        o.setField(ctx.ID().getText());
        return o;
    }
}
