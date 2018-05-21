package org.zstack.zql.ast.parser.visitors;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.zql.ASTNode;
import org.zstack.utils.DebugUtils;
import org.zstack.zql.antlr4.ZQLBaseVisitor;
import org.zstack.zql.antlr4.ZQLParser;

import java.util.stream.Collectors;

public class ValueVisitor extends ZQLBaseVisitor<ASTNode.Value> {
    public ASTNode.ComplexValue visitSubQueryValue(ZQLParser.SubQueryValueContext ctx) {
        ASTNode.ComplexValue v = new ASTNode.ComplexValue();
        v.setSubQuery(ctx.subQuery().accept(new SubQueryVisitor()));
        return v;
    }

    public ASTNode.Value visitValue(ZQLParser.ValueContext ctx) {
        if (!ctx.value().isEmpty()) {
            ASTNode.ListValue l = new ASTNode.ListValue();
            l.setValues(ctx.value().stream().map(it->it.accept(new ValueVisitor())).collect(Collectors.toList()));
            return l;
        }

        ASTNode.PlainValue v = new ASTNode.PlainValue();
        v.setText(ctx.getText());
        if (ctx.INT() != null) {
            v.setType(Long.class);
        } else if (ctx.FLOAT() != null) {
            v.setType(Double.class);
        } else if (ctx.STRING() != null) {
            v.setType(String.class);
        } else if (ctx.BOOLEAN() != null) {
            v.setType(Boolean.class);
            v.setText(StringUtils.strip(v.getText(), "'"));
        } else {
            DebugUtils.Assert(false, "should not be here");
        }

        v.setCtype(v.getType().getName());
        return v;
    }

    @Override
    public ASTNode.Value visitSimpleValue(ZQLParser.SimpleValueContext ctx) {
        return visitValue(ctx.value());
    }
}
