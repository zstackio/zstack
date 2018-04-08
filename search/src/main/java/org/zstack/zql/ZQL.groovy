package org.zstack.zql

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.apache.commons.lang.StringUtils
import org.zstack.core.db.SQLBatch
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger
import org.zstack.zql.antlr4.ZQLLexer
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode
import org.zstack.zql.ast.parser.visitors.CountVisitor
import org.zstack.zql.ast.parser.visitors.QueryVisitor
import org.zstack.zql.ast.visitors.result.QueryResult

import javax.persistence.Query

class ZQL {
    private static final CLogger logger = Utils.getLogger(ZQL.class)

    private QueryResult astResult
    private String text

    static class ThrowingErrorListener extends BaseErrorListener {
        String text

        ThrowingErrorListener(String t) {
            text = t
        }

        private String getTextAround(int lineNum, int pos) {
            String line = text.split("\n")[lineNum-1]
            int start = pos - 10 < 0 ? 0 : pos - 10
            int end = pos + 10 >= line.length() ? line.length() - 1 : pos + 10
            return line[start..end]
        }

        @Override
        void syntaxError(Recognizer recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("error ${msg} at line ${line}:${charPositionInLine}, ${getTextAround(line, charPositionInLine)}")
        }
    }

    static String queryTargetNameFromInventoryClass(Class invClass) {
        String name = invClass.simpleName.toLowerCase()
        return StringUtils.removeEnd(name, "inventory")
    }

    static ZQL fromString(String text) {
        if (logger.isTraceEnabled()) {
            logger.trace("created ZQL from text: ${text}")
        }

        return new ZQL(text: text)
    }

    private List entityVOtoInventories(List vos) {
        String collectionMethodName = astResult.inventoryMetadata.inventoryAnnotation.collectionValueOfMethod()
        if (collectionMethodName == "") {
            collectionMethodName = "valueOf"
        }

        return astResult.inventoryMetadata.selfInventoryClass.invokeMethod(collectionMethodName, vos)
    }

    ZQLQueryResult execute() {
        Long count = null
        List vos = null
        ZQLLexer l = new ZQLLexer(CharStreams.fromString(text))
        ZQLParser p = new ZQLParser(new CommonTokenStream(l))
        p.addErrorListener(new ThrowingErrorListener(text))

        ZQLParser.ZqlContext ctx = p.zql()
        if (ctx instanceof ZQLParser.CountGrammarContext) {
            ASTNode.Query query = ctx.count().accept(new CountVisitor())
            astResult = query.accept(new org.zstack.zql.ast.visitors.QueryVisitor(countQuery: true)) as QueryResult
            new SQLBatch() {
                @Override
                protected void scripts() {
                    Query q = astResult.createCountQuery(databaseFacade.getEntityManager()) as Query
                    count = q.getSingleResult() as Long
                }
            }.execute()
        } else if (ctx instanceof ZQLParser.QueryGrammarContext) {
            ASTNode.Query query = ctx.query().accept(new QueryVisitor())
            astResult = query.accept(new org.zstack.zql.ast.visitors.QueryVisitor()) as QueryResult
            new SQLBatch() {
                @Override
                protected void scripts() {
                    Query q = astResult.createJPAQuery(databaseFacade.getEntityManager()) as Query
                    vos = q.getResultList()

                    if (astResult.createCountQuery != null) {
                        q = astResult.createCountQuery(databaseFacade.getEntityManager()) as Query
                        count = q.getSingleResult() as Long
                    }
                }
            }.execute()
        } else {
            assert false : "should not be here ${ctx}"
        }

        ZQLQueryResult ret = new ZQLQueryResult(
                inventories: vos != null ? entityVOtoInventories(vos) : null,
                total: count
        )

        return ret
    }

    @Override
    String toString() {
        return astResult.sql
    }
}
