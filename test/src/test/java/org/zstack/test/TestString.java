package org.zstack.test;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.aspectj.weaver.ast.Or;
import org.hibernate.mapping.ValueVisitor;
import org.junit.Test;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.antlr4.ZQLBaseListener;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLLexer;
import org.zstack.zql.antlr4.ZQLParser;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class TestString {
    CLogger logger = Utils.getLogger(TestString.class);

    static class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

    static class ASTNode {
    }

    static class QueryTarget extends ASTNode {
        public Entity entity;
        public Field field;
    }

    static class Entity extends ASTNode {
        public String id;
    }

    static class Field extends ASTNode {
        public List<String> fields;
    }

    interface Value {
    }

    static class ComplexValue implements Value {
        public SubQuery subQuery;
    }

    static class PlainValue implements Value {
        public String text;
        public transient Class type;
        public String ctype;
    }

    static class ListValue implements Value {
        public List<Value> values;
    }

    interface Condition {
    }

    static class ExprNode extends ASTNode implements Condition {
        public String operator;
        public Field left;
        public Value right;
    }

    static class LogicalOperatorNode extends ASTNode implements Condition {
        public String operator;
        public Condition left;
        public Condition right;
    }

    static class OrderBy extends ASTNode {
        public String field;
        public String direction;
    }

    static class Limit extends ASTNode {
        public int limit;
    }

    static class Offset extends ASTNode {
        public int offset;
    }

    static class RestrictExpr extends ASTNode {
        public String entity;
        public String field;
        public String operator;
        public Value value;
    }

    static class RestrictBy extends ASTNode {
        public List<RestrictExpr> exprs;
    }

    static class ReturnWithExpr extends ASTNode {
        public List<String> names;
    }

    static class ReturnWith extends ASTNode {
        public List<ReturnWithExpr> exprs;
    }

    static class Query extends ASTNode {
        public QueryTarget target;
        public List<Condition> conditions;
        public RestrictBy restrictBy;
        public ReturnWith returnWith;
        public OrderBy orderBy;
        public Limit limit;
        public Offset offset;
    }

    static class SubQueryTarget extends ASTNode {
        public String entity;
        public List<String> fields;
    }

    static class SubQuery extends ASTNode {
        public SubQueryTarget target;
        public List<Condition> conditions;
    }

    public class CaseChangingCharStream implements CharStream {

        final CharStream stream;
        final boolean upper;

        /**
         * Constructs a new CaseChangingCharStream wrapping the given {@link CharStream} forcing
         * all characters to upper case or lower case.
         * @param stream The stream to wrap.
         * @param upper If true force each symbol to upper case, otherwise force to lower.
         */
        public CaseChangingCharStream(CharStream stream, boolean upper) {
            this.stream = stream;
            this.upper = upper;
        }

        @Override
        public String getText(Interval interval) {
            return stream.getText(interval);
        }

        @Override
        public void consume() {
            stream.consume();
        }

        @Override
        public int LA(int i) {
            int c = stream.LA(i);
            if (c <= 0) {
                return c;
            }
            if (upper) {
                return Character.toUpperCase(c);
            }
            return Character.toLowerCase(c);
        }

        @Override
        public int mark() {
            return stream.mark();
        }

        @Override
        public void release(int marker) {
            stream.release(marker);
        }

        @Override
        public int index() {
            return stream.index();
        }

        @Override
        public void seek(int index) {
            stream.seek(index);
        }

        @Override
        public int size() {
            return stream.size();
        }

        @Override
        public String getSourceName() {
            return stream.getSourceName();
        }
    }


    class EntityVisitor extends ZQLBaseVisitor<Entity> {
        @Override
        public Entity visitEntity(ZQLParser.EntityContext ctx) {
            Entity e = new Entity();
            e.id = ctx.ID().getText();
            return e;
        }
    }

    class FieldVisitor extends ZQLBaseVisitor<Field> {
        @Override public Field visitField(ZQLParser.FieldContext ctx) {
            Field f = new Field();
            f.fields = ctx.ID().stream().map(ParseTree::getText).collect(Collectors.toList());
            return f;
        }
    }

    class QueryTargetVisitor extends ZQLBaseVisitor<QueryTarget> {
        @Override
        public QueryTarget visitQueryTarget(ZQLParser.QueryTargetContext ctx) {
            QueryTarget qt = new QueryTarget();
            qt.entity = ctx.entity().accept(new EntityVisitor());
            if (ctx.field() != null) {
                qt.field = ctx.field().accept(new FieldVisitor());
            }
            return qt;
        }
    }

    class SubQueryVisitor extends ZQLBaseVisitor<SubQuery> {
        @Override
        public SubQuery visitSubQuery(ZQLParser.SubQueryContext ctx) {
            SubQuery sq = new SubQuery();
            sq.target = ctx.subQueryTarget().accept(new SubQueryTargetVisitor());
            sq.conditions = ctx.condition().stream().map(m->m.accept(new ConditionVisitor())).collect(Collectors.toList());
            return sq;
        }
    }

    class ValueVisitor extends ZQLBaseVisitor<Value> {
        @Override
        public ComplexValue visitSubQueryValue(ZQLParser.SubQueryValueContext ctx) {
            ComplexValue value = new ComplexValue();
            value.subQuery = ctx.subQuery().accept(new SubQueryVisitor());
            return value;
        }

        @Override
        public Value visitSimpleValue(ZQLParser.SimpleValueContext ctx) {
            return visitValue(ctx.value());
        }

        @Override
        public Value visitValue(ZQLParser.ValueContext ctx) {
            if (ctx.value() != null && !ctx.value().isEmpty()) {
                ListValue value = new ListValue();
                value.values = ctx.value().stream().map(v->v.accept(new ValueVisitor())).collect(Collectors.toList());
                return value;
            }

            PlainValue value = new PlainValue();
            value.text = ctx.getText();
            if (ctx.INT() != null) {
                value.type = Long.class;
            } else if (ctx.FLOAT() != null) {
                value.type = Double.class;
            } else if (ctx.STRING() != null) {
                value.type = String.class;
            } else {
                DebugUtils.Assert(false, "should not be here");
            }

            value.ctype = value.type.getSimpleName();

            return value;
        }
    }

    class ExprVisitor extends ZQLBaseVisitor<ExprNode> {
        @Override
        public ExprNode visitExpr(ZQLParser.ExprContext ctx) {
            ExprNode expr = new ExprNode();
            expr.left = ctx.field().accept(new FieldVisitor());
            expr.operator = ctx.operator().getText();
            if (ctx.complexValue() != null) {
                expr.right = ctx.complexValue().accept(new ValueVisitor());
            }
            return expr;
        }
    }

    class ConditionVisitor extends ZQLBaseVisitor<Condition> {
        @Override
        public Condition visitNestCondition(ZQLParser.NestConditionContext ctx) {
            LogicalOperatorNode ln = new LogicalOperatorNode();
            ln.left = ctx.left.accept(new ConditionVisitor());
            if (ctx.op != null) {
                ln.operator = ctx.op.getText();
                ln.right = ctx.right.accept(new ConditionVisitor());
            }

            return ln;
        }

        @Override
        public Condition visitSimpleCondition(ZQLParser.SimpleConditionContext ctx) {
            return ctx.expr().accept(new ExprVisitor());
        }
        /**
         * {@inheritDoc}
         *
         * <p>The default implementation returns the result of calling
         * {@link #visitChildren} on {@code ctx}.</p>
         */
        @Override
        public Condition visitParenthesisCondition(ZQLParser.ParenthesisConditionContext ctx) {
            return ctx.condition().accept(new ConditionVisitor());
        }
    }

    class OrderByVisitor extends ZQLBaseVisitor<OrderBy> {
        @Override
        public OrderBy visitOrderBy(ZQLParser.OrderByContext ctx) {
            OrderBy ob = new OrderBy();
            ob.direction = ctx.ORDER_BY_VALUE().getText();
            ob.field = ctx.ID().getText();
            return ob;
        }
    }

    class LimitVisitor extends ZQLBaseVisitor<Limit> {
        @Override
        public Limit visitLimit(ZQLParser.LimitContext ctx) {
            Limit l = new Limit();
            l.limit = Long.valueOf(ctx.INT().getText()).intValue();
            return l;
        }
    }

    class OffsetVisitor extends ZQLBaseVisitor<Offset> {
        @Override
        public Offset visitOffset(ZQLParser.OffsetContext ctx) {
            Offset o = new Offset();
            o.offset = Long.valueOf(ctx.INT().getText()).intValue();
            return o;
        }
    }

    class RestrictExprVisitor extends ZQLBaseVisitor<RestrictExpr> {
        @Override
        public RestrictExpr visitRestrictByExpr(ZQLParser.RestrictByExprContext ctx) {
            RestrictExpr expr = new RestrictExpr();
            expr.entity = ctx.entity().getText();
            expr.field = ctx.ID().getText();
            expr.operator = ctx.operator().getText();
            if (ctx.value() != null) {
                expr.value = ctx.value().accept(new ValueVisitor());
            }
            return expr;
        }
    }

    class RestrictByVisitor extends ZQLBaseVisitor<RestrictBy> {
        @Override
        public RestrictBy visitRestrictBy(ZQLParser.RestrictByContext ctx) {
            RestrictBy by = new RestrictBy();
            by.exprs = ctx.restrictByExpr().stream().map(e->e.accept(new RestrictExprVisitor())).collect(Collectors.toList());
            return by;
        }
    }

    class ReturnWithExprVisitor extends ZQLBaseVisitor<ReturnWithExpr> {
        @Override
        public ReturnWithExpr visitReturnWithExpr(ZQLParser.ReturnWithExprContext ctx) {
            ReturnWithExpr expr = new ReturnWithExpr();
            expr.names = ctx.ID().stream().map(ParseTree::getText).collect(Collectors.toList());
            return expr;
        }
    }

    class ReturnWithVisitor extends ZQLBaseVisitor<ReturnWith> {
        @Override
        public ReturnWith visitReturnWith(ZQLParser.ReturnWithContext ctx) {
            ReturnWith rw = new ReturnWith();
            rw.exprs = ctx.returnWithExpr().stream().map(r->r.accept(new ReturnWithExprVisitor())).collect(Collectors.toList());
            return rw;
        }
    }

    class SubQueryTargetVisitor extends ZQLBaseVisitor<SubQueryTarget> {
        @Override public SubQueryTarget visitSubQueryTarget(ZQLParser.SubQueryTargetContext ctx) {
            SubQueryTarget t = new SubQueryTarget();
            t.entity = ctx.entity().getText();
            t.fields = ctx.ID().stream().map(ParseTree::getText).collect(Collectors.toList());
            return t;
        }
    }

    class QueryVisitor extends ZQLBaseVisitor<Query> {
        @Override
        public Query visitQuery(ZQLParser.QueryContext ctx) {
            Query q = new Query();
            q.target = ctx.queryTarget().accept(new QueryTargetVisitor());
            q.conditions = ctx.condition().stream().map(c->c.accept(new ConditionVisitor())).collect(Collectors.toList());
            if (ctx.returnWith() != null) {
                q.returnWith = ctx.returnWith().accept(new ReturnWithVisitor());
            }
            if (ctx.restrictBy() != null) {
                q.restrictBy = ctx.restrictBy().accept(new RestrictByVisitor());
            }
            if (ctx.orderBy() != null) {
                q.orderBy = ctx.orderBy().accept(new OrderByVisitor());
            }
            if (ctx.limit() != null) {
                q.limit = ctx.limit().accept(new LimitVisitor());
            }
            if (ctx.offset() != null) {
                q.offset = ctx.offset().accept(new OffsetVisitor());
            }
            return q;
        }
    }

    @Test
    public void test() throws InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String text = "query vm.vmNics.id where ((uuid = 23 and name = \"hello\") or " +
                "description not null) and (nic.id = 523.2 or volume.size > 1000) " +
                "or vm.rootVolumeUuid = (query volume.uuid where name = \"root\" and type != \"Data\" or uuid in " +
                "(query volume.uuid where uuid not in (\"a5576d5e57a7443894eeb078702023fd\", \"36239a01763d4b4f8ad7cfdd0dc26f5f\"))) " +
                "restrict by (zone.uuid = \"8b78f4d7367c41dd86ebdd59052af8b9\", image.size > 100) " +
                "return with (count, metric.CPUIdleUtilization) " +
                "order by uuid desc limit 10 offset 10000 ";
        //String text = "query vm.vmNics.id where (uuid = 23 and name = \"hello\") or description not null and nic.id = 523.2";
        //String text = "query vm.vmNics.id where uuid = 23 and name = \"hello\" and description not null and nic.id = 523.2";

        CaseChangingCharStream s = new CaseChangingCharStream(CharStreams.fromString(text), true);
        ZQLLexer l = new ZQLLexer(CharStreams.fromString(text));
        ZQLParser p = new ZQLParser(new CommonTokenStream(l));
        p.addErrorListener(new ThrowingErrorListener());
        p.addParseListener(new ZQLBaseListener());
        Query query = p.query().accept(new QueryVisitor());
        logger.debug(String.format("xxxxxxxxxxxxxxxxxx: \n%s", JSONObjectUtil.toJsonString(query)));
        //p.zql();
    }
}
