package org.zstack.zql

import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.zstack.zql.antlr4.ZQLLexer
import org.zstack.zql.antlr4.ZQLParser
import org.zstack.zql.ast.ASTNode

import org.zstack.zql.ast.parser.visitors.QueryVisitor
import org.zstack.zql.ast.visitors.result.QueryResult

class ZQL {
    private QueryResult astResult

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

    static ZQL fromString(String text) {
        ZQLLexer l = new ZQLLexer(CharStreams.fromString(text))
        ZQLParser p = new ZQLParser(new CommonTokenStream(l))
        p.addErrorListener(new ThrowingErrorListener(text))
        ASTNode.Query query = p.query().accept(new QueryVisitor())
        def ret = query.accept(new QueryVisitor())
        return new ZQL(astResult: ret)
    }

    @Override
    String toString() {
        return astResult.sql
    }
}
