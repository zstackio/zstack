package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by MaJin on 2019/6/3.
 */
public class QueryTargetWithFunctionVisitor extends ZQLBaseVisitor<ASTNode.QueryTargetWithFunction> {
    @Override
    public ASTNode.QueryTargetWithFunction visitWithFunction(ZQLParser.WithFunctionContext ctx) {
        ASTNode.QueryTargetWithFunction q = new ASTNode.QueryTargetWithFunction();
        q.setFunction(ctx.function().accept(new FunctionVistor()));
        q.setSubTarget(ctx.queryTargetWithFunction().accept(new QueryTargetWithFunctionVisitor()));
        if (ctx.fieldWithFunction() != null) {
            List<ASTNode.FieldWithFunction> ff = ctx.fieldWithFunction().stream().map(it ->
                    (ASTNode.FieldWithFunction) it.accept(new FieldWithFunctionVisitor())).collect(Collectors.toList());
            q.setFieldsWithFunction(ff);
        }
        return q;
    }

    @Override
    public ASTNode.QueryTargetWithFunction visitWithoutFunction(ZQLParser.WithoutFunctionContext ctx) {
        return ASTNode.QueryTargetWithFunction.valueOf(ctx.queryTarget().accept(new QueryTargetVisitor()));
    }
}
