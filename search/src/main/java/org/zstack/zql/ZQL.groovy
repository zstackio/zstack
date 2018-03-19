package org.zstack.zql

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.zstack.zql.antlr4.ZQLLexer
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode
import org.zstack.zql.ast.ASTVisitor
import org.zstack.zql.ast.ParserVisitor

class ZQL {
    private ASTVisitor.QueryResult astResult

    static class ThrowingErrorListener extends BaseErrorListener {
        @Override
        void syntaxError(Recognizer recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e)
                throws ParseCancellationException {
            throw new ParseCancellationException("line " + line + ":" + charPositionInLine + " " + msg)
        }
    }

    static ZQL fromString(String text) {
        ZQLLexer l = new ZQLLexer(CharStreams.fromString(text))
        ZQLParser p = new ZQLParser(new CommonTokenStream(l))
        p.addErrorListener(new ThrowingErrorListener())
        ASTNode.Query query = p.query().accept(new ParserVisitor.QueryVisitor())
        def ret = query.accept(new ASTVisitor.QueryVisitor())
        return new ZQL(astResult: ret)
    }

    @Override
    String toString() {
        return astResult.sql
    }
}
