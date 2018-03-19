package org.zstack.zql.ast

import org.zstack.utils.DebugUtils
import org.zstack.zql.antlr4.ZQLBaseVisitor
import org.zstack.zql.antlr4.ZQLParser
import static org.zstack.zql.ast.ASTNode.*

class ParserVisitor {
    static class QueryTargetVisitor extends ZQLBaseVisitor<QueryTarget> {
        QueryTarget visitQueryTarget(ZQLParser.QueryTargetContext ctx) {
            return new QueryTarget(
                    entity: ctx.entity().ID().getText(),
                    fields: ctx.field() == null ?: ctx.field().ID().collect { it.getText() }
            )
        }
    }

    static class SubQueryVisitor extends ZQLBaseVisitor<SubQuery> {
        SubQuery visitSubQuery(ZQLParser.SubQueryContext ctx) {
            return new SubQuery(
                    target: ctx.subQueryTarget().accept(new SubQueryTargetVisitor()),
                    conditions: ctx.condition().collect {it.accept(new ConditionVisitor())}
            )
        }
    }

    static class ValueVisitor extends ZQLBaseVisitor<ASTNode.Value>  {
        ComplexValue visitSubQueryValue(ZQLParser.SubQueryValueContext ctx) {
            return new ComplexValue(subQuery: ctx.subQuery().accept(new SubQueryVisitor()))
        }

        ASTNode.Value visitValue(ZQLParser.ValueContext ctx) {
            if (!ctx.value()?.isEmpty()) {
                return new ListValue(values: ctx.value().collect {it.accept(new ValueVisitor())})
            }

            PlainValue v = new PlainValue(text: ctx.getText())
            if (ctx.INT() != null) {
                v.type = Long.class
            } else if (ctx.FLOAT() != null) {
                v.type = Double.class
            } else if (ctx.STRING() != null) {
                v.type = String.class
            } else {
                DebugUtils.Assert(false, "should not be here")
            }

            v.ctype = v.type.name
            return v
        }

        @Override
        ASTNode.Value visitSimpleValue(ZQLParser.SimpleValueContext ctx) {
            return visitValue(ctx.value())
        }
    }

    static class ExprVisitor extends ZQLBaseVisitor<Expr> {
        @Override
        Expr visitExpr(ZQLParser.ExprContext ctx) {
            return new Expr(
                    left: ctx.field().ID().collect {it.getText()},
                    operator: ctx.operator().getText(),
                    right: ctx.complexValue() == null ?: ctx.complexValue().accept(new ValueVisitor())
            )
        }
    }

    static class ConditionVisitor extends ZQLBaseVisitor<ASTNode.Condition> {
        @Override
        ASTNode.Condition visitNestCondition(ZQLParser.NestConditionContext ctx) {
            return new LogicalOperator(
                    left: ctx.left.accept(new ConditionVisitor()),
                    operator: ctx.op == null ?: ctx.op.getText(),
                    right: ctx.right == null ?: ctx.right.accept(new ConditionVisitor())
            )
        }

        ASTNode.Condition visitSimpleCondition(ZQLParser.SimpleConditionContext ctx) {
            return ctx.expr().accept(new ExprVisitor())
        }

        @Override
        ASTNode.Condition visitParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx) {
            return ctx.condition().accept(new ConditionVisitor())
        }
    }

    static class OrderByVisitor extends ZQLBaseVisitor<OrderBy> {
        @Override
        OrderBy visitOrderBy(ZQLParser.OrderByContext ctx) {
            return new OrderBy(
                    direction: ctx.ORDER_BY_VALUE().getText(),
                    field: ctx.ID().getText()
            )
        }
    }

    static class LimitVisitor extends ZQLBaseVisitor<Limit> {
        @Override
        Limit visitLimit(ZQLParser.LimitContext ctx) {
            return new Limit(limit: Long.valueOf(ctx.INT().getText()).intValue())
        }
    }

    static class OffsetVisitor extends ZQLBaseVisitor<Offset> {
        @Override
        Offset visitOffset(ZQLParser.OffsetContext ctx) {
            return new Offset(offset: Long.valueOf(ctx.INT().getText()).intValue())
        }
    }

    static class RestrictExprVisitor extends ZQLBaseVisitor<RestrictExpr> {
        @Override
        RestrictExpr visitRestrictByExpr(ZQLParser.RestrictByExprContext ctx) {
            return new RestrictExpr(
                    entity: ctx.entity().getText(),
                    field: ctx.ID().getText(),
                    operator: ctx.operator().getText(),
                    value: ctx.value() == null ?: ctx.value().accept(new ValueVisitor())
            )
        }
    }

    static class RestrictByVisitor extends ZQLBaseVisitor<RestrictBy> {
        @Override
        RestrictBy visitRestrictBy(ZQLParser.RestrictByContext ctx) {
            return new RestrictBy(exprs: ctx.restrictByExpr().collect {it.accept(new RestrictExprVisitor())})
        }
    }

    static class ReturnWithExprVisitor extends ZQLBaseVisitor<ReturnWithExpr> {
        @Override
        ReturnWithExpr visitReturnWithExpr(ZQLParser.ReturnWithExprContext ctx) {
            return new ReturnWithExpr(names: ctx.ID().collect {it.getText()})
        }
    }

    static class ReturnWithVisitor extends ZQLBaseVisitor<ReturnWith> {
        @Override
        ReturnWith visitReturnWith(ZQLParser.ReturnWithContext ctx) {
            return new ReturnWith(exprs: ctx.returnWithExpr().collect {it.accept(new ReturnWithExprVisitor())})
        }
    }

    static class SubQueryTargetVisitor extends ZQLBaseVisitor<SubQueryTarget> {
        @Override
        SubQueryTarget visitSubQueryTarget(ZQLParser.SubQueryTargetContext ctx) {
            return new SubQueryTarget(
                    entity: ctx.entity().getText(),
                    fields: ctx.ID().collect {it.getText()}
            )
        }
    }

    static class FilterByExprVisitor extends ZQLBaseVisitor<FilterByExpr> {
        @Override
        FilterByExpr visitFilterByExpr(ZQLParser.FilterByExprContext ctx) {
            return new FilterByExpr(
                    filterName: ctx.ID().getText(),
                    content: ctx.filterByExprBlock().getText()
            )
        }
    }

    static class FilterByVisitor extends ZQLBaseVisitor<FilterBy> {
        @Override
        FilterBy visitFilterBy(ZQLParser.FilterByContext ctx) {
            return new FilterBy(exprs: ctx.filterByExpr().collect {it.accept(new FilterByExprVisitor())})
        }
    }

    static class QueryVisitor extends ZQLBaseVisitor<Query> {
        @Override
        Query visitQuery(ZQLParser.QueryContext ctx) {
            return new Query(
                    target: ctx.queryTarget().accept(new QueryTargetVisitor()),
                    conditions: ctx.condition()?.collect {it.accept(new ConditionVisitor())},
                    filterBy: ctx.filterBy()?.accept(new FilterByVisitor()),
                    returnWith: ctx.returnWith()?.accept(new ReturnWithVisitor()),
                    restrictBy: ctx.restrictBy()?.accept(new RestrictByVisitor()),
                    orderBy: ctx.orderBy()?.accept(new OrderByVisitor()),
                    limit: ctx.limit()?.accept(new LimitVisitor()),
                    offset: ctx.offset()?.accept(new OffsetVisitor())
            )
        }
    }
}
