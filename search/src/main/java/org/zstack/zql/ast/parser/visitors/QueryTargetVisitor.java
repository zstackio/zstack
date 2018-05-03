package org.zstack.zql.ast.parser.visitors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class QueryTargetVisitor extends ZQLBaseVisitor<ASTNode.QueryTarget> {
    public ASTNode.QueryTarget visitQueryTarget(ZQLParser.QueryTargetContext ctx) {
        ASTNode.QueryTarget q = new ASTNode.QueryTarget();
        q.setEntity(ctx.entity().ID().getText());
        q.setFields(ctx.field() == null ? null : ctx.field().ID().stream().map(ParseTree::getText).collect(Collectors.toList()));
        return q;
    }
}
