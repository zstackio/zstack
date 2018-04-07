package org.zstack.zql.ast.visitors

import org.zstack.zql.ZQLContext
import org.zstack.zql.ast.ASTNode

class ValueVisitor implements ASTVisitor<String, ASTNode> {
    @Override
    String visit(ASTNode node) {
        if (node instanceof ASTNode.ListValue) {
            List<String> values = node.values.collect {
                assert it instanceof ASTNode.PlainValue

                if (ZQLContext.quoteStringValue && String.class.isAssignableFrom(it.type)) {
                    return "'${it.text}'"
                } else {
                    return it.text
                }
            }

            return "(${values.join(",")})"
        } else if (node instanceof ASTNode.PlainValue) {
            if (ZQLContext.quoteStringValue && String.class.isAssignableFrom(node.type)) {
                return "'${node.text}'"
            } else {
                return node.text
            }
        } else if (node instanceof ASTNode.ComplexValue) {
            return node.accept(new ComplexValueVisitor())
        } else {
            assert false : "should not be here, ${node.class}"
        }
    }
}
