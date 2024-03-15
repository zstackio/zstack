package org.zstack.zql.ast.parser.visitors;

import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class JoinClauseVisitor extends ZQLBaseVisitor<ASTNode.JoinClause> {

    @Override
    public ASTNode.JoinClause visitJoinTable(ZQLParser.JoinTableContext ctx) {
        String joinType;
        if (ctx.INNER() != null) {
            joinType = ctx.INNER().getText();
        } else if (ctx.LEFT() != null) {
            joinType = ctx.LEFT().getText();
        } else if (ctx.RIGHT() != null) {
            joinType = ctx.RIGHT().getText();
        } else {
            throw new OperationFailureException(Platform.operr("can not find JoinType"));
        }

        ASTNode.JoinClause outerJoin = new ASTNode.JoinClause();
        outerJoin.setJoinType(joinType);
        outerJoin.setJoin(ctx.JOIN().getText());
        outerJoin.setQueryTarget(ctx.queryTarget().accept(new QueryTargetVisitor()));
        outerJoin.setOn(ctx.ON().getText());
        outerJoin.setConditions(ctx.condition().stream()
                .map(it -> it.accept(new ConditionVisitor()))
                .collect(Collectors.toList()));
        return outerJoin;
    }
}
