package org.zstack.zql.ast.parser.visitors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class FieldWithFunctionVisitor extends ZQLBaseVisitor<ASTNode.FieldWithFunction> {
    @Override
    public ASTNode.FieldWithFunction visitFieldWithFunction(ZQLParser.FieldWithFunctionContext ctx) {
        ASTNode.FieldWithFunction q = new ASTNode.FieldWithFunction();
        if (ctx.function() != null) {
            q.setFunction(ctx.function().accept(new FunctionVistor()));
        }

        if (ctx.multiFields() != null) {
            q.setFields(ctx.multiFields().ID().stream().map(ParseTree::getText).collect(Collectors.toList()));
        }

        if (ctx.fieldWithFunction() != null) {
            q.setSubFieldWithFunction(ctx.fieldWithFunction().accept(new FieldWithFunctionVisitor()));
        }
        return q;
    }
}
