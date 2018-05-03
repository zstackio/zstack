package org.zstack.zql.ast.visitors;

import org.zstack.header.zql.ASTNode;
import org.zstack.header.zql.ASTVisitor;
import org.zstack.zql1.ast.visitors.ComplexValueVisitor;

import java.util.List;

public class ValueVisitor implements ASTVisitor<String, ASTNode> {
    @Override
    String visit(ASTNode node) {
        if (node instanceof ASTNode.ListValue) {
            List<String> values = node.values.collect {
                assert it instanceof ASTNode.PlainValue
                return it.text
            }

            return "(${values.join(",")})"
        } else if (node instanceof ASTNode.PlainValue) {
            return "${node.text}"
        } else if (node instanceof ASTNode.ComplexValue) {
            return node.accept(new ComplexValueVisitor())
        } else {
            assert false : "should not be here, ${node.class}"
        }
    }
}
