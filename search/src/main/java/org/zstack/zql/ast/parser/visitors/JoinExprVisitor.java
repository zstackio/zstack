package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

public class JoinExprVisitor extends ZQLBaseVisitor<ASTNode.JoinExpr> {
    @Override
    public ASTNode.JoinExpr visitJoinExpr(ZQLParser.JoinExprContext ctx) {
        ASTNode.JoinExpr e = new ASTNode.JoinExpr();
        e.setLeft(ctx.left.accept(new QueryTargetVisitor()));
        e.setOperator(ctx.operator().getText());
        e.setRight(ctx.right.accept(new QueryTargetVisitor()));
        return e;
    }
}
