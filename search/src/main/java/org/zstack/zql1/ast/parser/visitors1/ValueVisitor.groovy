package org.zstack.zql1.ast.parser.visitors1

import org.zstack.utils.DebugUtils
import org.zstack.zql1.antlr4.ZQLBaseVisitor
import org.zstack.zql1.antlr4.ZQLParser
import org.zstack.header.zql.ASTNode

class ValueVisitor extends ZQLBaseVisitor<ASTNode.Value> {
    ASTNode.ComplexValue visitSubQueryValue(ZQLParser.SubQueryValueContext ctx) {
        return new ASTNode.ComplexValue(subQuery: ctx.subQuery().accept(new SubQueryVisitor()))
    }

    ASTNode.Value visitValue(ZQLParser.ValueContext ctx) {
        if (!ctx.value()?.isEmpty()) {
            return new ASTNode.ListValue(values: ctx.value().collect {it.accept(new ValueVisitor())})
        }

        ASTNode.PlainValue v = new ASTNode.PlainValue(text: ctx.getText())
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
