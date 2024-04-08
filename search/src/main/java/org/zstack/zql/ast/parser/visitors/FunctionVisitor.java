package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

/**
 * Created by MaJin on 2019/6/3.
 */
public class FunctionVisitor extends ZQLBaseVisitor<ASTNode.Function> {
    @Override
    public ASTNode.Function visitFunction(ZQLParser.FunctionContext ctx) {
        ASTNode.Function f = new ASTNode.Function();
        f.setFunctionName(ctx.ID().getText());
        return f;
    }
}
