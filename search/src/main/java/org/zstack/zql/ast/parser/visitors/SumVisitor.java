package org.zstack.zql.ast.parser.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class SumVisitor extends ZQLBaseVisitor<ASTNode.Sum> {
    @Override
    public ASTNode.Sum visitSum(ZQLParser.SumContext ctx) {
        ASTNode.Sum sum = new ASTNode.Sum();
        sum.setTarget(ctx.queryTarget().accept(new QueryTargetVisitor()));
        sum.setConditions(ctx.condition() == null ? null : ctx.condition().stream().map(c->c.accept(new ConditionVisitor())).collect(Collectors.toList()));
        sum.setGroupByField(ctx.sumBy().sumByValue().getText());
        sum.setName(ctx.namedAs() == null ? null : ctx.namedAs().accept(new NamedAsVisitor()));
        return sum;
    }
}
