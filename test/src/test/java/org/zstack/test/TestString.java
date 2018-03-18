package org.zstack.test;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
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

    interface LogicalOperatorNode {
    }

    static class Value {
        public String text;
        public Class type;
    }

    interface Condition {
    }

    static class SingleCondition extends ASTNode implements Condition {
        public String operator;
        public Field left;
        public Value right;
    }

    static class And extends ASTNode implements LogicalOperatorNode, Condition {
        public Condition left;
        public Condition right;
    }

    static class Or extends ASTNode implements LogicalOperatorNode, Condition {
        public Condition left;
        public Condition right;
    }

    static class Query extends ASTNode {
        public QueryTarget target;
        public List<Condition> conditions;
    }

    @Test
    public void test() throws InterruptedException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String text = "query vm.vmNics.id where ((uuid = 23 and name = \"hello\") or description not null) and nic.id = 523.2";

        ZQLLexer l = new ZQLLexer(CharStreams.fromString(text));
        ZQLParser p = new ZQLParser(new CommonTokenStream(l));
        p.addErrorListener(new ThrowingErrorListener());

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

        class QueryVisitor extends ZQLBaseVisitor<Query> {
            @Override
            public Query visitQuery(ZQLParser.QueryContext ctx) {
                Query q = new Query();
                q.target = ctx.queryTarget().accept(new QueryTargetVisitor());
                return q;
            }
        }

        p.addParseListener(new ZQLBaseListener());
        Query query = p.query().accept(new QueryVisitor());
        logger.debug(String.format("xxxxxxxxxxxxxxxxxx: \n%s", JSONObjectUtil.toJsonString(query)));
        //p.zql();
    }
}
