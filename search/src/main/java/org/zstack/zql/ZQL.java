package org.zstack.zql;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vo.ToInventory;
import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.MarshalZQLASTTreeExtensionPoint;
import org.zstack.header.zql.ReturnWithExtensionPoint;
import org.zstack.header.zql.ZQLCustomizeContextExtensionPoint;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.antlr4.ZQLLexer;
import org.zstack.zql.antlr4.ZQLParser;
import org.zstack.zql.ast.ZQLMetadata;
import org.zstack.zql.ast.parser.visitors.CountVisitor;
import org.zstack.zql.ast.visitors.QueryVisitor;
import org.zstack.zql.ast.visitors.result.QueryResult;

import javax.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ZQL {
    private static final CLogger logger = Utils.getLogger(ZQL.class);

    private QueryResult astResult;
    private String text;

    @Autowired
    private PluginRegistry pluginRgty;

    static class ThrowingErrorListener extends BaseErrorListener {
        String text;

        ThrowingErrorListener(String t) {
            text = t;
        }

        private String getTextAround(int lineNum, int pos) {
            String line = text.split("\n")[lineNum-1];
            int start = pos - 10 < 0 ? 0 : pos - 10;
            int end = pos + 10 >= line.length() ? line.length() - 1 : pos + 10;
            return line.substring(start, end);
        }

        @Override
        public void syntaxError(Recognizer recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException(String.format("error %s at line %s:%s, %s",
                    msg, line, charPositionInLine, getTextAround(line, charPositionInLine)));
        }
    }

    public static String queryTargetNameFromInventoryClass(Class invClass) {
        String name = invClass.getSimpleName().toLowerCase();
        return StringUtils.removeEnd(name, "inventory");
    }

    public static ZQL fromString(String text) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("created ZQL from text: %s", text));
        }

        ZQL zql = new ZQL();
        zql.text = text;
        return zql;
    }

    private List entityVOtoInventories(List vos) {
        List ret = new ArrayList();
        if (astResult.targetFieldNames != null && !astResult.targetFieldNames.isEmpty()) {
            vos.forEach(it -> {
                try {
                    Object inv = astResult.inventoryMetadata.selfInventoryClass.getConstructor().newInstance();
                    if (it instanceof Object[]) {
                        Object[] fieldValues = (Object[]) it;
                        for (int i = 0; i < fieldValues.length; i++) {
                            BeanUtils.setProperty(inv, astResult.targetFieldNames.get(i), fieldValues[i]);
                        }
                    } else {
                        BeanUtils.setProperty(inv, astResult.targetFieldNames.get(0), it);
                    }
                    ret.add(inv);
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                }
            });
        } else {
            vos.forEach(it -> ret.add(ToInventory.toInventory(it)));
        }

        return ret;
    }

    private static void callExtensions(ASTNode.Query node) {
        Platform.getComponentLoader().getComponent(PluginRegistry.class)
                .getExtensionList(MarshalZQLASTTreeExtensionPoint.class)
                .forEach(it -> it.marshalZQLASTTree(node));
    }

    private Runnable prepareZQLContext(ASTNode.Query node) {
        ZQLMetadata.InventoryMetadata inventory = ZQLMetadata.findInventoryMetadata(node.getTarget().getEntity());
        org.zstack.zql.ZQLContext.setQueryTargetInventoryName(inventory.fullInventoryName());

        List<Runnable> cleanUps = new ArrayList<>();
        pluginRgty.getExtensionList(ZQLCustomizeContextExtensionPoint.class).forEach(e->{
            Runnable r = e.zqlCustomizeContext(node);
            if (r != null) {
                cleanUps.add(r);
            }
        });

        return () -> {
            cleanUps.forEach(Runnable::run);
            ZQLContext.cleanQueryTargetInventoryName();
        };
    }

    public ZQLQueryReturn execute() {
        ZQLQueryReturn qr = new ZQLQueryReturn();

        class Ret {
            Long count;
            List vos;
        }

        Ret ret = new Ret();

        ZQLLexer l = new ZQLLexer(CharStreams.fromString(text));
        ZQLParser p = new ZQLParser(new CommonTokenStream(l));
        p.addErrorListener(new ThrowingErrorListener(text));

        ZQLParser.ZqlContext ctx = p.zql();

        if (ctx instanceof ZQLParser.CountGrammarContext) {
            ASTNode.Query query = ((ZQLParser.CountGrammarContext)ctx).count().accept(new CountVisitor());

            Runnable clean = prepareZQLContext(query);

            callExtensions(query);

            astResult = (QueryResult) query.accept(new QueryVisitor(true));

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("ZQL query: %s", astResult.sql));
            }

            new SQLBatch() {
                @Override
                protected void scripts() {
                    Query q = astResult.createCountQuery.apply(databaseFacade.getEntityManager());
                    ret.count = (Long) q.getSingleResult();
                }
            }.execute();

            clean.run();
        } else if (ctx instanceof ZQLParser.QueryGrammarContext) {
            ASTNode.Query query = (( ZQLParser.QueryGrammarContext)ctx).query().accept(new org.zstack.zql.ast.parser.visitors.QueryVisitor());

            Runnable clean = prepareZQLContext(query);

            callExtensions(query);
            astResult = (QueryResult) query.accept(new QueryVisitor(false));

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("ZQL query: %s", astResult.sql));
            }

            new SQLBatch() {
                @Override
                protected void scripts() {
                    Query q = astResult.createJPAQuery.apply(databaseFacade.getEntityManager());
                    ret.vos = q.getResultList();

                    if (astResult.createCountQuery != null) {
                        q = astResult.createCountQuery.apply(databaseFacade.getEntityManager());
                        ret.count = (Long) q.getSingleResult();
                    }
                }
            }.execute();

            qr.returnWith = callReturnWithExtensions(astResult, ret.vos);

            clean.run();
        } else {
            throw new CloudRuntimeException(String.format("should not be here, %s", ctx));
        }

        qr.inventories = ret.vos != null ? entityVOtoInventories(ret.vos) : null;
        qr.total = ret.count;

        return qr;
    }

    private Map callReturnWithExtensions(QueryResult astResult, List vos) {
        if (astResult.returnWith == null || astResult.returnWith.isEmpty()) {
            return null;
        }

        Map ret = new HashMap();
        astResult.returnWith.forEach(r -> {
            Optional<ReturnWithExtensionPoint> opt = pluginRgty.getExtensionList(ReturnWithExtensionPoint.class)
                    .stream().filter(ext->r.name.equals(ext.getReturnWithName())).findAny();
            if (!opt.isPresent()) {
                throw new CloudRuntimeException(String.format("cannot find any ReturnWithExtensionPoint dealing with %s", r.name));
            }

            ReturnWithExtensionPoint ext = opt.get();
            ext.returnWith(vos, r.expr, ret);
        });

        return ret;
    }

    @Override
    public String toString() {
        return astResult.sql;
    }
}
